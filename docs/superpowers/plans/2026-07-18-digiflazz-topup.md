# Digiflazz Prepaid Topup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `RealProviderAdapter` stub with a real Digiflazz prepaid topup call, make the purchase flow three-state (SUCCESS/PENDING/FAILED), and settle Pending transactions with an outbound reconcile poll — no public webhook.

**Architecture:** `ProviderPort` gains a `refId` (the transaction UUID → Digiflazz `ref_id`). `DigiflazzClient` (supplier slice) owns the `POST /v1/transaction` protocol; `RealProviderAdapter` maps its result to a three-state `ProviderResponse`. `TransactionDomainService.createPurchase` routes on status via an extracted `reconcileProviderResult`. A `@Scheduled` `TransactionReconcileService` re-polls stale PROCESSING rows (re-POST same `ref_id`, idempotent) and settles them through the same method. The charged `walletId` is persisted on the transaction row so the scheduled job can refund without request context.

**Tech Stack:** Spring Boot 4, Java 25, Hibernate/JPA (PostgreSQL), `RestClient`, Jackson, JUnit 5 + AssertJ + Mockito, `MockRestServiceServer`.

## Global Constraints

- Architecture: layered vertical slice; ports/adapters ONLY at external boundaries. Provider = external → `ProviderPort` stays. (spec)
- Money-safe mapping: unknown / parse-failure / `Gagal`+rc∈{`01`,`50`} → **PENDING**, never auto-refund. Only `Gagal`+terminal-rc refunds. (spec)
- TDD strict: red → green. No implementation before a failing test. Run full suite before done.
- `git rm` for deletions/renames (never plain `rm`).
- Never expose `e.getMessage()` to clients; `log.error()` only.
- DDL-auto = `update` in dev, `validate` in prod → new columns need `ALTER TABLE` in prod (note in commit).
- Digiflazz sign = `md5(username + apiKey + ref_id)` for `/transaction`; POST, `Content-Type: application/json`.
- Build/test against `supplier.mode=mock` (default); real egress IP whitelist is out of scope.
- Run tests with: `./mvnw -pl satset-core test -Dtest=<ClassName>` (single) or `./mvnw -pl satset-core test` (full).

---

### Task 1: Provider status model + `refId` signature (compile-safe refactor)

Introduce `ProviderStatus`, make `ProviderResponse` carry it, add `refId` to
`ProviderPort`. Pure refactor — behavior identical, existing suite stays green.

**Files:**
- Create: `satset-core/src/main/java/com/satset/transaction/model/ProviderStatus.java`
- Modify: `satset-core/src/main/java/com/satset/transaction/model/ProviderResponse.java`
- Modify: `satset-core/src/main/java/com/satset/transaction/client/ProviderPort.java`
- Modify: `satset-core/src/main/java/com/satset/transaction/client/MockProviderAdapter.java`
- Modify: `satset-core/src/main/java/com/satset/supplier/client/RealProviderAdapter.java`
- Modify: `satset-core/src/main/java/com/satset/transaction/service/TransactionDomainService.java:98`
- Modify (tests): `satset-core/src/test/java/com/satset/transaction/service/TransactionDomainServiceTest.java`

**Interfaces:**
- Produces: `enum ProviderStatus { SUCCESS, PENDING, FAILED }`
- Produces: `ProviderResponse(ProviderStatus status, String referenceNumber, String serialNumber, String message, BigDecimal cost)` with `boolean success()` → `status == SUCCESS`
- Produces: `ProviderPort.sendTransaction(String targetNumber, String denomCode, BigDecimal amount, String refId)`

- [ ] **Step 1: Create the enum**

`ProviderStatus.java`:
```java
package com.satset.transaction.model;

public enum ProviderStatus {
    SUCCESS, PENDING, FAILED
}
```

- [ ] **Step 2: Rewrite `ProviderResponse` to carry status**

```java
package com.satset.transaction.model;

import java.math.BigDecimal;

public record ProviderResponse(
        ProviderStatus status,
        String referenceNumber,
        String serialNumber,
        String message,
        BigDecimal cost) {

    /** True only for a settled-successful transaction. */
    public boolean success() {
        return status == ProviderStatus.SUCCESS;
    }
}
```

- [ ] **Step 3: Add `refId` to the port**

`ProviderPort.java`:
```java
package com.satset.transaction.client;

import com.satset.transaction.model.ProviderResponse;

import java.math.BigDecimal;

public interface ProviderPort {

    ProviderResponse sendTransaction(String targetNumber, String denomCode,
                                     BigDecimal amount, String refId);
}
```

- [ ] **Step 4: Update `MockProviderAdapter` (signature + enum result)**

Change the method signature to add `String refId` and build responses with the
enum:
```java
    @Override
    public ProviderResponse sendTransaction(String targetNumber, String denomCode,
                                            BigDecimal amount, String refId) {
        log.info("Mock provider: sending {} {} to {} (ref={})", denomCode, amount, targetNumber, refId);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean success = Math.random() < 0.9;

        if (success) {
            String ref = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String sn = "SN" + System.currentTimeMillis();
            log.info("Mock provider: SUCCESS ref={} sn={}", ref, sn);
            return new ProviderResponse(ProviderStatus.SUCCESS, ref, sn, "Transaksi berhasil", null);
        }

        log.warn("Mock provider: FAILED for target={}", targetNumber);
        return new ProviderResponse(ProviderStatus.FAILED, null, null, "Transaksi gagal dari provider", null);
    }
```
Add `import com.satset.transaction.model.ProviderStatus;`.

- [ ] **Step 5: Update `RealProviderAdapter` signature (stub still throws)**

Only change the method signature (add `String refId`) — leave the
`UnsupportedOperationException` body for now; Task 4 fills it:
```java
    @Override
    public ProviderResponse sendTransaction(String targetNumber, String denomCode,
                                            BigDecimal amount, String refId) {
        throw new UnsupportedOperationException(
                "Kontrak API supplier belum ada — pakai supplier.mode=mock sampai dok turun");
    }
```

- [ ] **Step 6: Update the call site in `createPurchase`**

`TransactionDomainService.java` line ~98 — pass the transaction id as `refId`:
```java
                ProviderResponse response = providerService.sendTransaction(
                                targetNumber, denom.code(), total, transaction.getId().toString());
```

- [ ] **Step 7: Update `TransactionDomainServiceTest` stubs/constructions**

This file stubs `sendTransaction(...)` and constructs `ProviderResponse(...)` in
several places. Apply these two mechanical replacements everywhere they appear:

1. Every `sendTransaction(anyString(), anyString(), any(BigDecimal.class))` →
   `sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString())`
   (5 occurrences: lines ~84, ~139, ~165, ~185, ~205, ~259, ~337; also the
   `verify(...).sendTransaction(...)` at ~123 and the positional
   `verify(...).sendTransaction("081234567890", "TLKM10", new BigDecimal("10000.00"))`
   at ~95 → add `, anyString()` as a 4th arg: `..., new BigDecimal("10000.00"), anyString())`).
2. `new ProviderResponse(true, ...)` → `new ProviderResponse(ProviderStatus.SUCCESS, ...)`;
   `new ProviderResponse(false, ...)` → `new ProviderResponse(ProviderStatus.FAILED, ...)`.

Add `import com.satset.transaction.model.ProviderStatus;` (the file already
`import com.satset.transaction.model.*;` — so no new import needed).

- [ ] **Step 8: Run the suite to verify green**

Run: `./mvnw -pl satset-core test -Dtest=TransactionDomainServiceTest`
Expected: PASS (all existing tests, behavior unchanged).

- [ ] **Step 9: Commit**

```bash
git add satset-core/src/main/java/com/satset/transaction/model/ProviderStatus.java \
        satset-core/src/main/java/com/satset/transaction/model/ProviderResponse.java \
        satset-core/src/main/java/com/satset/transaction/client/ProviderPort.java \
        satset-core/src/main/java/com/satset/transaction/client/MockProviderAdapter.java \
        satset-core/src/main/java/com/satset/supplier/client/RealProviderAdapter.java \
        satset-core/src/main/java/com/satset/transaction/service/TransactionDomainService.java \
        satset-core/src/test/java/com/satset/transaction/service/TransactionDomainServiceTest.java
git commit -m "refactor(transaction): three-state ProviderStatus + refId on ProviderPort"
```

---

### Task 2: Persist `walletId` on the transaction

The reconcile job runs without request context, so the charged wallet must live
on the row.

**Files:**
- Modify: `satset-core/src/main/java/com/satset/transaction/model/Transactions.java`
- Modify: `satset-core/src/main/java/com/satset/transaction/service/TransactionDomainService.java`
- Test: `satset-core/src/test/java/com/satset/transaction/service/TransactionDomainServiceTest.java`

**Interfaces:**
- Produces: `Transactions.getWalletId()` / `setWalletId(String)` (Lombok `@Data`)

- [ ] **Step 1: Write the failing test**

Add to `TransactionDomainServiceTest`:
```java
    @Test
    void createPurchase_PersistsWalletId() throws InsufficientBalanceException {
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(inv -> {
            Transactions tx = inv.getArgument(0);
            if (tx.getId() == null) tx.setId(UUID.randomUUID());
            return tx;
        });
        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new ProviderResponse(ProviderStatus.SUCCESS, "REF-1", "SN-1", "OK", null));

        transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

        ArgumentCaptor<Transactions> captor = ArgumentCaptor.forClass(Transactions.class);
        verify(transactionRepository, atLeastOnce()).save(captor.capture());
        assertEquals(walletId, captor.getValue().getWalletId());
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl satset-core test -Dtest=TransactionDomainServiceTest#createPurchase_PersistsWalletId`
Expected: FAIL — `getWalletId()` does not exist (compile error).

- [ ] **Step 3: Add the column**

In `Transactions.java`, add after the `storeId` field:
```java
    @Column(name = "wallet_id", length = 50)
    private String walletId;
```

- [ ] **Step 4: Set it in `createPurchase`**

In the "1. Create transaction (PENDING)" block, after `transaction.setStoreId(storeId);`:
```java
                transaction.setWalletId(walletId);
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw -pl satset-core test -Dtest=TransactionDomainServiceTest#createPurchase_PersistsWalletId`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add satset-core/src/main/java/com/satset/transaction/model/Transactions.java \
        satset-core/src/main/java/com/satset/transaction/service/TransactionDomainService.java \
        satset-core/src/test/java/com/satset/transaction/service/TransactionDomainServiceTest.java
git commit -m "feat(transaction): persist wallet_id on transactions (prod needs ALTER TABLE)"
```

---

### Task 3: `DigiflazzClient.topup` — real `POST /v1/transaction`

**Files:**
- Modify: `satset-core/src/main/java/com/satset/supplier/client/DigiflazzClient.java`
- Test: `satset-core/src/test/java/com/satset/supplier/client/DigiflazzClientTopupTest.java` (create)

**Interfaces:**
- Produces: `record DigiTxResult(String status, String rc, String refId, String sn, BigDecimal price, String message)` (public, in `DigiflazzClient`)
- Produces: `DigiTxResult DigiflazzClient.topup(String refId, String buyerSkuCode, String customerNo)`

- [ ] **Step 1: Write the failing test**

`DigiflazzClientTopupTest.java`:
```java
package com.satset.supplier.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DigiflazzClientTopupTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private DigiflazzClient client;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient rc = builder.build();
        // username="u", apiKey="k" -> sign = md5("u" + "k" + "ref1")
        client = new DigiflazzClient(rc, "https://api.digiflazz.com/v1", "u", "k");
    }

    @Test
    void topup_sendsSignedRequest_parsesPending() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
              .andExpect(method(org.springframework.http.HttpMethod.POST))
              .andExpect(jsonPath("$.buyer_sku_code").value("xld25"))
              .andExpect(jsonPath("$.customer_no").value("0878"))
              .andExpect(jsonPath("$.ref_id").value("ref1"))
              // md5("ukref1") = precomputed below
              .andExpect(jsonPath("$.sign").value("d8f0e2b1c3a4..."))  // replace with real md5 (Step 3a)
              .andRespond(withSuccess("""
                  {"data":{"ref_id":"ref1","customer_no":"0878","buyer_sku_code":"xld25",
                  "message":"Transaksi Pending","status":"Pending","rc":"03","sn":"",
                  "buyer_last_saldo":100000,"price":25000}}
                  """, MediaType.APPLICATION_JSON));

        var r = client.topup("ref1", "xld25", "0878");

        assertThat(r.status()).isEqualTo("Pending");
        assertThat(r.rc()).isEqualTo("03");
        assertThat(r.price()).isEqualByComparingTo(new BigDecimal("25000"));
        server.verify();
    }

    @Test
    void topup_parsesSukses() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
              .andRespond(withSuccess("""
                  {"data":{"ref_id":"ref1","status":"Sukses","rc":"00",
                  "sn":"SN123","price":24500,"message":"Sukses"}}
                  """, MediaType.APPLICATION_JSON));

        var r = client.topup("ref1", "xld25", "0878");

        assertThat(r.status()).isEqualTo("Sukses");
        assertThat(r.sn()).isEqualTo("SN123");
        assertThat(r.price()).isEqualByComparingTo(new BigDecimal("24500"));
    }

    @Test
    void topup_malformedBody_returnsUnknownStatus() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
              .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        var r = client.topup("ref1", "xld25", "0878");

        assertThat(r.status()).isNull();   // adapter maps null-status -> PENDING (money-safe)
        assertThat(r.rc()).isEqualTo("PARSE");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl satset-core test -Dtest=DigiflazzClientTopupTest`
Expected: FAIL — `topup` / `DigiTxResult` do not exist (compile error).

- [ ] **Step 3a: Compute the real sign for the test**

Run: `printf 'ukref1' | md5sum` (or `md5 -s ukref1`) and paste the hex into the
`$.sign` matcher in Step 1 (replace `d8f0e2b1c3a4...`).

- [ ] **Step 3b: Implement `topup` + `DigiTxResult`**

Add to `DigiflazzClient` (reusing the existing `sign(...)`, `MAPPER`, `http`,
`baseUrl`, `username`):
```java
    /** Result of a /transaction call (topup or status re-query). Supplier-local — no transaction types leak. */
    public record DigiTxResult(String status, String rc, String refId,
                               String sn, BigDecimal price, String message) {}

    /**
     * Prepaid topup — POST /transaction. Idempotent per {@code refId}: re-calling with the
     * same refId returns the current status without re-charging (also used for status polling).
     * Sign = md5(username + apiKey + refId).
     */
    public DigiTxResult topup(String refId, String buyerSkuCode, String customerNo) {
        var req = new TransactionRequest(username, buyerSkuCode, customerNo, refId, sign(refId));
        String raw = http.post()
                .uri(baseUrl + "/transaction")
                .contentType(APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(String.class);
        try {
            JsonNode d = MAPPER.readTree(raw == null ? "" : raw).path("data");
            BigDecimal price = d.hasNonNull("price") ? d.get("price").decimalValue() : null;
            String status = d.path("status").isMissingNode() ? null : d.path("status").asText(null);
            return new DigiTxResult(status, d.path("rc").asText(""),
                    d.path("ref_id").asText(refId), d.path("sn").asText(""),
                    price, d.path("message").asText(""));
        } catch (Exception e) {
            log.error("Gagal parse respons Digiflazz /transaction refId={}", refId, e);
            return new DigiTxResult(null, "PARSE", refId, "", null, "Respons Digiflazz tidak valid");
        }
    }

    private record TransactionRequest(String username, String buyer_sku_code,
                                      String customer_no, String ref_id, String sign) {}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -pl satset-core test -Dtest=DigiflazzClientTopupTest`
Expected: PASS (all three).

- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/java/com/satset/supplier/client/DigiflazzClient.java \
        satset-core/src/test/java/com/satset/supplier/client/DigiflazzClientTopupTest.java
git commit -m "feat(supplier): DigiflazzClient.topup — signed POST /v1/transaction"
```

---

### Task 4: `RealProviderAdapter` — map DF result → three-state `ProviderResponse`

**Files:**
- Modify: `satset-core/src/main/java/com/satset/supplier/client/RealProviderAdapter.java`
- Test: `satset-core/src/test/java/com/satset/supplier/client/RealProviderAdapterTest.java` (create)

**Interfaces:**
- Consumes: `DigiflazzClient.topup(...)` → `DigiTxResult`; `ProviderStatus`, `ProviderResponse`
- Produces: real `RealProviderAdapter.sendTransaction(...)` behavior

- [ ] **Step 1: Write the failing test**

`RealProviderAdapterTest.java`:
```java
package com.satset.supplier.client;

import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.ProviderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RealProviderAdapterTest {

    private final DigiflazzClient df = mock(DigiflazzClient.class);
    private final RealProviderAdapter adapter = new RealProviderAdapter(df);

    private void stub(String status, String rc) {
        when(df.topup("ref1", "xld25", "0878")).thenReturn(
                new DigiflazzClient.DigiTxResult(status, rc, "ref1", "SN9", new BigDecimal("24500"), "msg"));
    }

    @Test void sukses_mapsSuccess() {
        stub("Sukses", "00");
        ProviderResponse r = adapter.sendTransaction("0878", "xld25", new BigDecimal("25000"), "ref1");
        assertThat(r.status()).isEqualTo(ProviderStatus.SUCCESS);
        assertThat(r.serialNumber()).isEqualTo("SN9");
        assertThat(r.cost()).isEqualByComparingTo("24500");
    }

    @Test void pending_mapsPending() {
        stub("Pending", "03");
        assertThat(adapter.sendTransaction("0878", "xld25", new BigDecimal("25000"), "ref1").status())
                .isEqualTo(ProviderStatus.PENDING);
    }

    @Test void gagalTimeout_mapsPending_moneySafe() {
        stub("Gagal", "01");   // timeout may have formed a tx -> do NOT refund
        assertThat(adapter.sendTransaction("0878", "xld25", new BigDecimal("25000"), "ref1").status())
                .isEqualTo(ProviderStatus.PENDING);
    }

    @Test void gagalNotFound_mapsPending_moneySafe() {
        stub("Gagal", "50");
        assertThat(adapter.sendTransaction("0878", "xld25", new BigDecimal("25000"), "ref1").status())
                .isEqualTo(ProviderStatus.PENDING);
    }

    @Test void gagalTerminal_mapsFailed() {
        stub("Gagal", "44");   // insufficient DF balance -> real failure -> refund
        assertThat(adapter.sendTransaction("0878", "xld25", new BigDecimal("25000"), "ref1").status())
                .isEqualTo(ProviderStatus.FAILED);
    }

    @Test void unknownStatus_mapsPending_moneySafe() {
        stub(null, "PARSE");
        assertThat(adapter.sendTransaction("0878", "xld25", new BigDecimal("25000"), "ref1").status())
                .isEqualTo(ProviderStatus.PENDING);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl satset-core test -Dtest=RealProviderAdapterTest`
Expected: FAIL — adapter throws `UnsupportedOperationException` / constructor mismatch.

- [ ] **Step 3: Implement the adapter**

Replace `RealProviderAdapter.java` body (keep the `@ConditionalOnProperty` and
class annotations):
```java
package com.satset.supplier.client;

import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.ProviderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Adapter supplier ASLI — delegasi ke {@link DigiflazzClient#topup} lalu memetakan
 * status/rc Digiflazz ke {@link ProviderStatus}. Aktif kalau {@code supplier.mode=real}.
 *
 * <p>Money-safe: status tak dikenal / Gagal dgn rc yang bisa membentuk transaksi
 * (timeout, not-found) dipetakan ke PENDING agar tidak auto-refund (poll yang menyelesaikan).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "supplier.mode", havingValue = "real")
public class RealProviderAdapter implements ProviderPort {

    // rc "Gagal" yang berarti transaksi benar-benar tidak terbentuk -> boleh refund.
    // rc timeout(01)/not-found(50) BISA membentuk transaksi -> jangan refund, biar poll yang settle.
    private static final Set<String> FORMS_TRANSACTION = Set.of("01", "50");

    private final DigiflazzClient digiflazz;

    public RealProviderAdapter(DigiflazzClient digiflazz) {
        this.digiflazz = digiflazz;
    }

    @Override
    public ProviderResponse sendTransaction(String targetNumber, String denomCode,
                                            BigDecimal amount, String refId) {
        var r = digiflazz.topup(refId, denomCode, targetNumber);
        ProviderStatus status = mapStatus(r.status(), r.rc());
        log.info("Digiflazz /transaction refId={} status={} rc={} -> {}", refId, r.status(), r.rc(), status);
        return new ProviderResponse(status, r.refId(), emptyToNull(r.sn()), r.message(), r.price());
    }

    private static ProviderStatus mapStatus(String dfStatus, String rc) {
        if ("Sukses".equalsIgnoreCase(dfStatus)) return ProviderStatus.SUCCESS;
        if ("Gagal".equalsIgnoreCase(dfStatus) && !FORMS_TRANSACTION.contains(rc)) return ProviderStatus.FAILED;
        // Pending, Gagal+forms-transaction, null/unknown -> PENDING (poll resolves)
        return ProviderStatus.PENDING;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -pl satset-core test -Dtest=RealProviderAdapterTest`
Expected: PASS (all six).

- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/java/com/satset/supplier/client/RealProviderAdapter.java \
        satset-core/src/test/java/com/satset/supplier/client/RealProviderAdapterTest.java
git commit -m "feat(supplier): RealProviderAdapter maps Digiflazz status/rc (money-safe PENDING)"
```

---

### Task 5: `createPurchase` PENDING branch + extract `reconcileProviderResult`

**Files:**
- Modify: `satset-core/src/main/java/com/satset/transaction/service/TransactionDomainService.java`
- Test: `satset-core/src/test/java/com/satset/transaction/service/TransactionDomainServiceTest.java`

**Interfaces:**
- Produces: `void reconcileProviderResult(Transactions tx, ProviderResponse r, String walletId, DenomInfo denom)` (package-private, reused by Task 6)

- [ ] **Step 1: Write the failing test**

Add to `TransactionDomainServiceTest`:
```java
    @Test
    void createPurchase_Pending_StaysProcessing_NoRefund() throws InsufficientBalanceException {
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(inv -> {
            Transactions tx = inv.getArgument(0);
            if (tx.getId() == null) tx.setId(UUID.randomUUID());
            return tx;
        });
        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new ProviderResponse(ProviderStatus.PENDING, "REF-P", null, "Transaksi Pending", null));

        TransactionDTO result = transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

        assertEquals(TransactionStatus.PROCESSING, result.status());
        assertEquals("REF-P", result.providerRef());
        verify(balanceService, never()).refundBalance(any(), any(), any(), any());
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl satset-core test -Dtest=TransactionDomainServiceTest#createPurchase_Pending_StaysProcessing_NoRefund`
Expected: FAIL — current code treats non-success as FAILED→refund, status ends REFUNDED/FAILED.

- [ ] **Step 3: Extract the settle block and add the PENDING branch**

In `TransactionDomainService.createPurchase`, replace the whole
`if (response.success()) { … } else { … }` block (lines ~101–133) with:
```java
                reconcileProviderResult(transaction, response, walletId, denom);
```
Then add this method to the class (below `createPurchase`):
```java
        /**
         * Settle a PROCESSING transaction against a provider result. Shared by the
         * purchase flow and the reconcile poll.
         * <ul>
         *   <li>SUCCESS  → mark SUCCESS, snapshot ref/sn/cost/margin.
         *   <li>PENDING  → leave PROCESSING, keep providerRef, NO refund (poll settles later).
         *   <li>FAILED   → refund; on refund failure leave FAILED for manual Ops.
         * </ul>
         */
        void reconcileProviderResult(Transactions transaction, ProviderResponse response,
                        String walletId, DenomInfo denom) {
                if (response.status() == ProviderStatus.PENDING) {
                        if (response.referenceNumber() != null) {
                                transaction.setProviderRef(response.referenceNumber());
                        }
                        transactionRepository.save(transaction); // stays PROCESSING
                        log.info("Transaction PENDING: id={} ref={} — awaiting reconcile",
                                        transaction.getId(), response.referenceNumber());
                        return;
                }

                if (response.success()) {
                        transaction.setStatus(TransactionStatus.SUCCESS);
                        transaction.setProviderRef(response.referenceNumber());
                        transaction.setSerialNumber(response.serialNumber());
                        BigDecimal costPrice = response.cost() != null ? response.cost() : denom.basePrice();
                        transaction.setCostPrice(costPrice);
                        transaction.setMargin(costPrice != null
                                        ? transaction.getTotal().subtract(costPrice) : null);
                        transactionRepository.save(transaction);
                        log.info("Transaction SUCCESS: id={} ref={} sn={}",
                                        transaction.getId(), response.referenceNumber(), response.serialNumber());
                        return;
                }

                // FAILED → refund
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
                try {
                        balanceService.refundBalance(walletId, transaction.getTotal(),
                                        transaction.getId(),
                                        "Refund " + denom.name() + " - " + response.message());
                        transaction.setStatus(TransactionStatus.REFUNDED);
                        transactionRepository.save(transaction);
                        log.warn("Transaction REFUNDED: id={} reason={}",
                                        transaction.getId(), response.message());
                } catch (Exception e) {
                        log.error("ALERT: Failed to refund transaction {} for wallet {}. Reason: {}",
                                        transaction.getId(), walletId, e.getMessage(), e);
                }
        }
```
Note: the previous code used `denom.basePrice()` for the cost fallback and
`total.subtract(costPrice)` for margin — preserved above via
`transaction.getTotal()` (equal to `total`). Ensure `ProviderStatus` is imported
(the file already has `import com.satset.transaction.model.*;`).

- [ ] **Step 4: Run test to verify it passes (and no regressions)**

Run: `./mvnw -pl satset-core test -Dtest=TransactionDomainServiceTest`
Expected: PASS — new PENDING test plus all existing SUCCESS/FAILED/REFUNDED tests.

- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/java/com/satset/transaction/service/TransactionDomainService.java \
        satset-core/src/test/java/com/satset/transaction/service/TransactionDomainServiceTest.java
git commit -m "feat(transaction): PENDING keeps PROCESSING; extract reconcileProviderResult"
```

---

### Task 6: Reconcile poll for stale PROCESSING transactions

**Files:**
- Modify: `satset-core/src/main/java/com/satset/transaction/repository/TransactionRepository.java`
- Create: `satset-core/src/main/java/com/satset/transaction/service/TransactionReconcileService.java`
- Modify: `satset-core/src/main/resources/application.yml`
- Modify: main application class (add `@EnableScheduling` if absent)
- Test: `satset-core/src/test/java/com/satset/transaction/service/TransactionReconcileServiceTest.java` (create)

**Interfaces:**
- Consumes: `TransactionDomainService.reconcileProviderResult(...)` (Task 5); `ProviderPort` (Task 1); `DenomRepository.findDenomInfoById`; `Transactions.getWalletId()` (Task 2)
- Produces: `List<Transactions> TransactionRepository.findByStatusAndCreatedAtBefore(TransactionStatus, LocalDateTime, Pageable)`

- [ ] **Step 1: Add the repository query**

In `TransactionRepository`:
```java
    java.util.List<Transactions> findByStatusAndCreatedAtBefore(
            TransactionStatus status, java.time.LocalDateTime cutoff,
            org.springframework.data.domain.Pageable pageable);
```
(`Pageable` gives the batch cap via `PageRequest.of(0, batchSize)`.)

- [ ] **Step 2: Write the failing test**

`TransactionReconcileServiceTest.java`:
```java
package com.satset.transaction.service;

import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.model.*;
import com.satset.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionReconcileServiceTest {

    @Mock TransactionRepository txRepo;
    @Mock DenomRepository denomRepo;
    @Mock ProviderPort provider;
    @Mock TransactionDomainService txService;

    TransactionReconcileService reconcile;

    UUID denomId = UUID.randomUUID();
    DenomInfo denom = new DenomInfo(denomId, "xld25", "XL 25K", "XL",
            new BigDecimal("25000.00"), BigDecimal.ZERO, new BigDecimal("24500.00"), true, false);

    @BeforeEach
    void setUp() {
        reconcile = new TransactionReconcileService(txRepo, denomRepo, provider, txService, 120000L, 100);
    }

    private Transactions stale() {
        Transactions tx = new Transactions();
        tx.setId(UUID.randomUUID());
        tx.setWalletId("w1");
        tx.setProductDenomId(denomId);
        tx.setTargetNumber("0878");
        tx.setTotal(new BigDecimal("25000.00"));
        tx.setStatus(TransactionStatus.PROCESSING);
        return tx;
    }

    @Test
    void reconcile_repollsStale_andSettles() {
        Transactions tx = stale();
        when(txRepo.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(), any()))
                .thenReturn(List.of(tx));
        when(denomRepo.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        ProviderResponse resp = new ProviderResponse(ProviderStatus.SUCCESS, "REF", "SN", "Sukses", new BigDecimal("24500"));
        when(provider.sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx.getId().toString()))
                .thenReturn(resp);

        reconcile.reconcileStalePending();

        verify(provider).sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx.getId().toString());
        verify(txService).reconcileProviderResult(tx, resp, "w1", denom);
    }

    @Test
    void reconcile_empty_doesNothing() {
        when(txRepo.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(), any()))
                .thenReturn(List.of());

        reconcile.reconcileStalePending();

        verifyNoInteractions(provider, txService);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./mvnw -pl satset-core test -Dtest=TransactionReconcileServiceTest`
Expected: FAIL — `TransactionReconcileService` does not exist.

- [ ] **Step 4: Implement the service**

`TransactionReconcileService.java`:
```java
package com.satset.transaction.service;

import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.TransactionStatus;
import com.satset.transaction.model.Transactions;
import com.satset.transaction.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Settles PROCESSING transactions left in-flight by a Digiflazz "Pending" response.
 * Re-POSTs /transaction with the same ref_id (idempotent, no re-charge) and applies
 * the current status via {@link TransactionDomainService#reconcileProviderResult}.
 *
 * <p>ponytail: batch cap per run so a backlog can't stampede Digiflazz's rate limit
 * (rc 85). Widen {@code supplier.reconcile.batch-size} if throughput needs it.
 */
@Slf4j
@Service
public class TransactionReconcileService {

    private final TransactionRepository txRepo;
    private final DenomRepository denomRepo;
    private final ProviderPort provider;
    private final TransactionDomainService txService;
    private final long staleAfterMs;
    private final int batchSize;

    public TransactionReconcileService(
            TransactionRepository txRepo,
            DenomRepository denomRepo,
            ProviderPort provider,
            TransactionDomainService txService,
            @Value("${supplier.reconcile.stale-after-ms:120000}") long staleAfterMs,
            @Value("${supplier.reconcile.batch-size:100}") int batchSize) {
        this.txRepo = txRepo;
        this.denomRepo = denomRepo;
        this.provider = provider;
        this.txService = txService;
        this.staleAfterMs = staleAfterMs;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${supplier.reconcile.interval-ms:60000}")
    @Transactional
    public void reconcileStalePending() {
        LocalDateTime cutoff = LocalDateTime.now().minusNanos(staleAfterMs * 1_000_000);
        List<Transactions> stale = txRepo.findByStatusAndCreatedAtBefore(
                TransactionStatus.PROCESSING, cutoff, PageRequest.of(0, batchSize));
        if (stale.isEmpty()) return;

        log.info("Reconcile: {} stale PROCESSING tx", stale.size());
        for (Transactions tx : stale) {
            try {
                DenomInfo denom = denomRepo.findDenomInfoById(tx.getProductDenomId()).orElse(null);
                if (denom == null) {
                    log.warn("Reconcile skip: denom {} gone for tx {}", tx.getProductDenomId(), tx.getId());
                    continue;
                }
                ProviderResponse resp = provider.sendTransaction(
                        tx.getTargetNumber(), denom.code(), tx.getTotal(), tx.getId().toString());
                txService.reconcileProviderResult(tx, resp, tx.getWalletId(), denom);
            } catch (Exception e) {
                log.error("Reconcile error tx {}: {}", tx.getId(), e.getMessage(), e);
                // leave PROCESSING → retried next run
            }
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw -pl satset-core test -Dtest=TransactionReconcileServiceTest`
Expected: PASS (both).

- [ ] **Step 6: Enable scheduling + config**

If the main `@SpringBootApplication` class lacks `@EnableScheduling`, add it
(find with `grep -rl "@SpringBootApplication" satset-core/src/main/java`). Then
add to `application.yml` under `supplier:` (create the key if absent):
```yaml
supplier:
  mode: ${SUPPLIER_MODE:mock}
  reconcile:
    interval-ms: 60000
    stale-after-ms: 120000
    batch-size: 100
  digiflazz:
    base-url: https://api.digiflazz.com/v1
    username: ${DIGIFLAZZ_USERNAME:}
    api-key: ${DIGIFLAZZ_API_KEY:}
```
(Keep any existing `supplier.digiflazz.*` values — merge, don't duplicate.)

- [ ] **Step 7: Full-suite regression + commit**

Run: `./mvnw -pl satset-core test`
Expected: PASS (whole module — confirms the signature ripple + scheduling wiring
break nothing, incl. `PurchaseFlowIntegrationTest`).

```bash
git add satset-core/src/main/java/com/satset/transaction/repository/TransactionRepository.java \
        satset-core/src/main/java/com/satset/transaction/service/TransactionReconcileService.java \
        satset-core/src/main/resources/application.yml \
        satset-core/src/test/java/com/satset/transaction/service/TransactionReconcileServiceTest.java
# plus the main application class if @EnableScheduling was added
git commit -m "feat(transaction): scheduled reconcile poll settles stale PENDING topups"
```

---

## Self-Review

- **Spec coverage:** ProviderStatus (T1) · ProviderResponse status (T1) · ProviderPort+refId (T1) · walletId persistence 5b (T2) · DigiflazzClient.topup 4 (T3) · RealProviderAdapter map + rc table 5 (T4) · createPurchase PENDING + reconcileProviderResult 6 (T5) · reconcile poll 7 (T6) · config 8 (T6). Non-goals (webhook, egress IP, saldo check) intentionally absent.
- **Money-safe mapping:** enforced + tested in T4 (`Gagal`+01/50 → PENDING; +44 → FAILED; null → PENDING).
- **Type consistency:** `reconcileProviderResult(Transactions, ProviderResponse, String, DenomInfo)` defined T5, consumed T6 identically. `sendTransaction(..., String refId)` 4-arg consistent T1/T4/T6. `DigiTxResult` fields consistent T3/T4.
- **`PurchaseFlowIntegrationTest`:** exercised by the full-suite run in T6 step 7 (may construct `ProviderResponse` or stub `sendTransaction` — if it fails to compile, apply the same two replacements from T1 step 7 before committing T6).
