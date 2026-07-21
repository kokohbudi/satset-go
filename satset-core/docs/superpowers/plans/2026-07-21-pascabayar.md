# Pascabayar (Postpaid Bill Payment) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Two-step postpaid flow (cek tagihan → bayar) via Digiflazz `inq-pasca`/`pay-pasca`, with an open-amount (input nominal) branch, reusing the existing purchase grid UI.

**Architecture:** New `PostpaidService` in the `transaction` slice orchestrates inquiry and pay; `ProviderPort` gains `inquiry(...)`/`payPostpaid(...)` implemented by `RealProviderAdapter` delegating to new `DigiflazzClient` methods. Pay is self-contained: fresh ref → re-inquiry → mismatch guard (`BILL_CHANGED` → 409) → deduct → `pay-pasca` on the same ref → reuse existing `reconcileProviderResult`. No inquiry persistence, no new tables, one ledger (`Transactions` + nullable `customerName`).

**Tech Stack:** Java 25, Spring Boot 4, Maven (`./mvnw`), JPA/Hibernate, Thymeleaf + Alpine.js, JUnit 5 + AssertJ + Mockito + MockRestServiceServer.

## Global Constraints

- Tests: JUnit 5 + AssertJ + Mockito. Run one test: `./mvnw test -Dtest=ClassName#method`.
- Every commit message ends with `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` (second `-m`).
- Use `git rm` for deletes/renames (none are planned).
- The existing prepaid `TransactionRequest` record must NOT gain a `commands` field (Jackson would emit `"commands":null` on prepaid topups). Postpaid uses a separate `PascaRequest` record.
- `pay-pasca` NEVER sends `amount` — the ref-bound inquiry already fixed the bill.
- Amount rules (enforced in `PostpaidService`): `OPEN_AMOUNT` + `requiresInquiry` → amount REQUIRED and within `minAmount`/`maxAmount`, sent to DF in `inq-pasca`; `FIXED_DENOM` + `requiresInquiry` → amount MUST be null, DF `inq-pasca` sent WITHOUT amount.
- `BusinessException` is CHECKED (`extends Exception`, `getErrorCode()`/`getErrorMessage()`) → 400 via `GlobalExceptionHandler.handleBusinessException`; `SupplierException(code, message)` is runtime → 502. `BILL_CHANGED` must map to 409 (Task 8).
- Prod uses `ddl-auto=validate`: the `customer_name` column needs a manual `ALTER TABLE` in prod (dev `ddl-auto=update` auto-adds). Noted in Task 7.
- Never expose raw `e.getMessage()` to the client (log only).
- Existing prepaid direct-pay flow stays unchanged (UI and backend).
- Sign for all DF calls = `md5(username + apiKey + ref_id)`; with test creds `u`/`k`/`ref1` → `c28850e81191973e911ac305b9cc7c42`.

---

## Phase 1 — Backend inquiry

### Task 1: `DigiflazzClient.inquiry` (inq-pasca)

**Files:**
- Modify: `src/main/java/com/satset/digiflazz/client/DigiflazzClient.java`
- Test: `src/test/java/com/satset/digiflazz/client/DigiflazzClientPascaTest.java` (create)

**Interfaces:**
- Consumes: existing private `String sign(String cmdKeyword)`, existing fields (`RestClient`, `baseUrl`, `username`, `apiKey`, `testing`, static `MAPPER` with `FAIL_ON_UNKNOWN_PROPERTIES=false`), existing `public record DigiTxResult(String status, String rc, String refId, String sn, BigDecimal price, String message)`.
- Produces: `public DigiInquiryResult inquiry(String refId, String buyerSkuCode, String customerNo, BigDecimal amount)`; `public record DigiInquiryResult(String status, String rc, String refId, String customerName, BigDecimal price, BigDecimal admin, String sn, String message, JsonNode desc)`; private `@JsonInclude(NON_NULL) record PascaRequest(String commands, String username, String buyer_sku_code, String customer_no, String ref_id, String sign, boolean testing, BigDecimal amount)`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/satset/digiflazz/client/DigiflazzClientPascaTest.java` (mirror the setup style of the existing `DigiflazzClientTest` in the same package):

```java
package com.satset.digiflazz.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DigiflazzClientPascaTest {

    private MockRestServiceServer server;
    private DigiflazzClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DigiflazzClient(builder.build(), "https://api.digiflazz.com/v1", "u", "k", false);
    }

    @Test
    void inquirySendsInqPascaWithoutAmountAndParsesBill() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.commands").value("inq-pasca"))
                .andExpect(jsonPath("$.username").value("u"))
                .andExpect(jsonPath("$.buyer_sku_code").value("pln"))
                .andExpect(jsonPath("$.customer_no").value("530000000001"))
                .andExpect(jsonPath("$.ref_id").value("ref1"))
                .andExpect(jsonPath("$.sign").value("c28850e81191973e911ac305b9cc7c42"))
                .andExpect(jsonPath("$.amount").doesNotExist())
                .andRespond(withSuccess("""
                        {"data":{"ref_id":"ref1","customer_no":"530000000001",
                         "customer_name":"BUDI SANTOSO","buyer_sku_code":"pln",
                         "admin":2500,"price":145000,"selling_price":147500,
                         "rc":"00","status":"Sukses","message":"Inquiry Sukses",
                         "desc":{"tarif":"R1","daya":1300,"lembar_tagihan":1}}}
                        """, MediaType.APPLICATION_JSON));

        DigiflazzClient.DigiInquiryResult r = client.inquiry("ref1", "pln", "530000000001", null);

        assertThat(r.status()).isEqualTo("Sukses");
        assertThat(r.rc()).isEqualTo("00");
        assertThat(r.refId()).isEqualTo("ref1");
        assertThat(r.customerName()).isEqualTo("BUDI SANTOSO");
        assertThat(r.price()).isEqualByComparingTo("145000");
        assertThat(r.admin()).isEqualByComparingTo("2500");
        assertThat(r.desc().path("tarif").asText()).isEqualTo("R1");
    }

    @Test
    void inquirySendsAmountWhenProvided() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
                .andExpect(jsonPath("$.commands").value("inq-pasca"))
                .andExpect(jsonPath("$.amount").value(25000))
                .andRespond(withSuccess("""
                        {"data":{"ref_id":"ref1","customer_no":"0812345678","customer_name":"BUDI",
                         "buyer_sku_code":"gopay","admin":1000,"price":25000,"selling_price":26000,
                         "rc":"00","status":"Sukses","message":"Inquiry Sukses","desc":{}}}
                        """, MediaType.APPLICATION_JSON));

        DigiflazzClient.DigiInquiryResult r =
                client.inquiry("ref1", "gopay", "0812345678", new java.math.BigDecimal("25000"));

        assertThat(r.rc()).isEqualTo("00");
        assertThat(r.price()).isEqualByComparingTo("25000");
    }

    @Test
    void inquiryReturnsHttpRcOnTransportError() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
                .andRespond(withServerError());

        DigiflazzClient.DigiInquiryResult r = client.inquiry("ref1", "pln", "530000000001", null);

        assertThat(r.status()).isNull();
        assertThat(r.rc()).isEqualTo("HTTP");
    }

    @Test
    void inquiryReturnsParseRcOnGarbageBody() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
                .andRespond(withSuccess("not-json-at-all", MediaType.APPLICATION_JSON));

        DigiflazzClient.DigiInquiryResult r = client.inquiry("ref1", "pln", "530000000001", null);

        assertThat(r.rc()).isEqualTo("PARSE");
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -Dtest=DigiflazzClientPascaTest`
Expected: COMPILE FAILURE — `method inquiry(...)` and `DigiInquiryResult` do not exist.

- [ ] **Step 3: Write minimal implementation**

In `DigiflazzClient.java`, first open the file and mirror `topup()`'s exact HTTP call chain, private field names, and error-handling structure (the code below assumes the fields described by the existing `topup()`: the injected `RestClient`, `baseUrl`, `username`, `apiKey`, `testing`, static `MAPPER` — adapt identifier names to the file's actual ones, keep everything else identical). Do NOT touch `TransactionRequest`. Add:

```java
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
private record PascaRequest(String commands, String username, String buyer_sku_code,
        String customer_no, String ref_id, String sign, boolean testing, BigDecimal amount) {
}

public record DigiInquiryResult(String status, String rc, String refId, String customerName,
        BigDecimal price, BigDecimal admin, String sn, String message,
        com.fasterxml.jackson.databind.JsonNode desc) {
}

public DigiInquiryResult inquiry(String refId, String buyerSkuCode, String customerNo, BigDecimal amount) {
    PascaRequest request = new PascaRequest("inq-pasca", username, buyerSkuCode, customerNo,
            refId, sign(refId), testing, amount);
    try {
        String raw = restClient.post()
                .uri(baseUrl + "/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);
        JsonNode data = MAPPER.readTree(raw).path("data");
        return new DigiInquiryResult(
                data.path("status").asText(null),
                data.path("rc").asText(null),
                data.path("ref_id").asText(null),
                data.path("customer_name").asText(null),
                data.hasNonNull("price") ? data.get("price").decimalValue() : null,
                data.hasNonNull("admin") ? data.get("admin").decimalValue() : null,
                data.path("sn").asText(null),
                data.path("message").asText(null),
                data.path("desc"));
    } catch (RestClientException e) {
        return new DigiInquiryResult(null, "HTTP", refId, null, null, null, null, e.getMessage(), null);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
        return new DigiInquiryResult(null, "PARSE", refId, null, null, null, null, e.getMessage(), null);
    }
}
```

`@JsonInclude(NON_NULL)` on `PascaRequest` is what omits `amount` when null (asserted by `$.amount.doesNotExist()`).

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -Dtest=DigiflazzClientPascaTest`
Expected: PASS (4 tests). Also run `./mvnw test -Dtest=DigiflazzClientTest,DigiflazzClientTopupTest` — expected: PASS (prepaid untouched).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/satset/digiflazz/client/DigiflazzClient.java src/test/java/com/satset/digiflazz/client/DigiflazzClientPascaTest.java
git commit -m "feat(digiflazz): add inq-pasca inquiry call" -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2: `DigiflazzClient.payPostpaid` (pay-pasca)

**Files:**
- Modify: `src/main/java/com/satset/digiflazz/client/DigiflazzClient.java`
- Test: `src/test/java/com/satset/digiflazz/client/DigiflazzClientPascaTest.java` (extend)

**Interfaces:**
- Consumes: `PascaRequest` and `sign(...)` from Task 1; existing `DigiTxResult(String status, String rc, String refId, String sn, BigDecimal price, String message)`.
- Produces: `public DigiTxResult payPostpaid(String refId, String buyerSkuCode, String customerNo)` — `commands="pay-pasca"`, never sends `amount`.

- [ ] **Step 1: Write the failing tests** (add to `DigiflazzClientPascaTest`)

```java
@Test
void payPostpaidSendsPayPascaWithoutAmountAndParsesStruk() {
    server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.commands").value("pay-pasca"))
            .andExpect(jsonPath("$.ref_id").value("ref1"))
            .andExpect(jsonPath("$.sign").value("c28850e81191973e911ac305b9cc7c42"))
            .andExpect(jsonPath("$.amount").doesNotExist())
            .andRespond(withSuccess("""
                    {"data":{"ref_id":"ref1","customer_no":"530000000001","buyer_sku_code":"pln",
                     "admin":2500,"price":147500,"selling_price":149000,"rc":"00","status":"Sukses",
                     "sn":"STRUK/PLN/1234567890","message":"Pembayaran Sukses"}}
                    """, MediaType.APPLICATION_JSON));

    DigiflazzClient.DigiTxResult r = client.payPostpaid("ref1", "pln", "530000000001");

    assertThat(r.status()).isEqualTo("Sukses");
    assertThat(r.rc()).isEqualTo("00");
    assertThat(r.sn()).isEqualTo("STRUK/PLN/1234567890");
    assertThat(r.price()).isEqualByComparingTo("147500");
}

@Test
void payPostpaidReturnsHttpRcOnTransportError() {
    server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
            .andRespond(withServerError());

    DigiflazzClient.DigiTxResult r = client.payPostpaid("ref1", "pln", "530000000001");

    assertThat(r.status()).isNull();
    assertThat(r.rc()).isEqualTo("HTTP");
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -Dtest=DigiflazzClientPascaTest#payPostpaidSendsPayPascaWithoutAmountAndParsesStruk`
Expected: COMPILE FAILURE — `payPostpaid` does not exist.

- [ ] **Step 3: Minimal implementation** (in `DigiflazzClient.java`; parse block mirrors `topup()`'s `DigiTxResult` parsing — copy that block's field extraction verbatim)

```java
public DigiTxResult payPostpaid(String refId, String buyerSkuCode, String customerNo) {
    PascaRequest request = new PascaRequest("pay-pasca", username, buyerSkuCode, customerNo,
            refId, sign(refId), testing, null);
    try {
        String raw = restClient.post()
                .uri(baseUrl + "/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);
        JsonNode data = MAPPER.readTree(raw).path("data");
        return new DigiTxResult(
                data.path("status").asText(null),
                data.path("rc").asText(null),
                data.path("ref_id").asText(null),
                data.path("sn").asText(null),
                data.hasNonNull("price") ? data.get("price").decimalValue() : null,
                data.path("message").asText(null));
    } catch (RestClientException e) {
        return new DigiTxResult(null, "HTTP", refId, null, null, e.getMessage());
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
        return new DigiTxResult(null, "PARSE", refId, null, null, e.getMessage());
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./mvnw test -Dtest=DigiflazzClientPascaTest`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/satset/digiflazz/client/DigiflazzClient.java src/test/java/com/satset/digiflazz/client/DigiflazzClientPascaTest.java
git commit -m "feat(digiflazz): add pay-pasca postpaid payment call" -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 3: `InquiryResult` + `ProviderPort` additions + `RealProviderAdapter`

**Files:**
- Create: `src/main/java/com/satset/transaction/model/InquiryResult.java`
- Modify: `src/main/java/com/satset/transaction/client/ProviderPort.java`
- Modify: `src/main/java/com/satset/transaction/client/RealProviderAdapter.java` (sole implementer of `ProviderPort` — verified by grep)
- Test: `src/test/java/com/satset/transaction/client/RealProviderAdapterPostpaidTest.java` (create; mirror setup of existing `RealProviderAdapterTest`)

**Interfaces:**
- Consumes: `DigiflazzClient.inquiry(refId, buyerSkuCode, customerNo, amount)` / `DigiflazzClient.payPostpaid(refId, buyerSkuCode, customerNo)` (Tasks 1–2); existing `mapStatus(String dfStatus, String rc)` ("Sukses"→SUCCESS; "Gagal" && rc not in {"01","50"}→FAILED; else PENDING) and `emptyToNull` helper; existing `ProviderResponse(ProviderStatus status, String referenceNumber, String serialNumber, String message, BigDecimal cost)`.
- Produces: `public record InquiryResult(String customerName, BigDecimal bill, BigDecimal admin, String rc, String message, JsonNode desc)` with `public boolean ok() { return "00".equals(rc); }`; on `ProviderPort`: `InquiryResult inquiry(String customerNo, String denomCode, String refId, BigDecimal amount);` and `ProviderResponse payPostpaid(String customerNo, String denomCode, String refId);`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/satset/transaction/client/RealProviderAdapterPostpaidTest.java` (if the existing `RealProviderAdapterTest` constructs the adapter differently than `new RealProviderAdapter(df)`, mirror that file's construction exactly):

```java
package com.satset.transaction.client;

import com.satset.digiflazz.client.DigiflazzClient;
import com.satset.transaction.model.InquiryResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RealProviderAdapterPostpaidTest {

    private final DigiflazzClient df = mock(DigiflazzClient.class);
    private final RealProviderAdapter adapter = new RealProviderAdapter(df);

    @Test
    void inquiryMapsDigiResultToInquiryResult() {
        when(df.inquiry("ref1", "pln", "530000000001", null))
                .thenReturn(new DigiflazzClient.DigiInquiryResult("Sukses", "00", "ref1",
                        "BUDI SANTOSO", new BigDecimal("145000"), new BigDecimal("2500"),
                        null, "Inquiry Sukses", null));

        InquiryResult r = adapter.inquiry("530000000001", "pln", "ref1", null);

        assertThat(r.customerName()).isEqualTo("BUDI SANTOSO");
        assertThat(r.bill()).isEqualByComparingTo("145000");
        assertThat(r.admin()).isEqualByComparingTo("2500");
        assertThat(r.rc()).isEqualTo("00");
        assertThat(r.ok()).isTrue();
    }

    @Test
    void inquiryWithNonZeroRcIsNotOk() {
        when(df.inquiry("ref1", "pln", "530000000001", null))
                .thenReturn(new DigiflazzClient.DigiInquiryResult("Gagal", "14", "ref1",
                        null, null, null, null, "Nomor tidak ditemukan", null));

        InquiryResult r = adapter.inquiry("530000000001", "pln", "ref1", null);

        assertThat(r.ok()).isFalse();
        assertThat(r.message()).isEqualTo("Nomor tidak ditemukan");
    }

    @Test
    void payPostpaidMapsSuksesToSuccessWithStruk() {
        when(df.payPostpaid("ref1", "pln", "530000000001"))
                .thenReturn(new DigiflazzClient.DigiTxResult("Sukses", "00", "ref1",
                        "STRUK/PLN/1234567890", new BigDecimal("147500"), "Pembayaran Sukses"));

        ProviderResponse r = adapter.payPostpaid("530000000001", "pln", "ref1");

        assertThat(r.status()).isEqualTo(ProviderStatus.SUCCESS);
        assertThat(r.serialNumber()).isEqualTo("STRUK/PLN/1234567890");
        assertThat(r.cost()).isEqualByComparingTo("147500");
    }

    @Test
    void payPostpaidMapsGagalHardRcToFailed() {
        when(df.payPostpaid("ref1", "pln", "530000000001"))
                .thenReturn(new DigiflazzClient.DigiTxResult("Gagal", "99", "ref1",
                        null, null, "Gagal"));

        assertThat(adapter.payPostpaid("530000000001", "pln", "ref1").status())
                .isEqualTo(ProviderStatus.FAILED);
    }

    @Test
    void payPostpaidMapsHttpErrorToPending() {
        when(df.payPostpaid("ref1", "pln", "530000000001"))
                .thenReturn(new DigiflazzClient.DigiTxResult(null, "HTTP", "ref1",
                        null, null, "timeout"));

        assertThat(adapter.payPostpaid("530000000001", "pln", "ref1").status())
                .isEqualTo(ProviderStatus.PENDING);
    }
}
```

Note: if `ProviderStatus` / `ProviderResponse` live in a different sub-package, mirror the imports used by `RealProviderAdapterTest`.

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -Dtest=RealProviderAdapterPostpaidTest`
Expected: COMPILE FAILURE — `InquiryResult`, `ProviderPort.inquiry`, `payPostpaid` do not exist.

- [ ] **Step 3: Minimal implementation**

Create `src/main/java/com/satset/transaction/model/InquiryResult.java`:

```java
package com.satset.transaction.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record InquiryResult(String customerName, BigDecimal bill, BigDecimal admin,
        String rc, String message, JsonNode desc) {

    public boolean ok() {
        return "00".equals(rc);
    }
}
```

Add to `ProviderPort.java` (keeping the existing `sendTransaction` untouched):

```java
InquiryResult inquiry(String customerNo, String denomCode, String refId, BigDecimal amount);

ProviderResponse payPostpaid(String customerNo, String denomCode, String refId);
```

Add to `RealProviderAdapter.java` (reuse the existing `mapStatus` and `emptyToNull`):

```java
@Override
public InquiryResult inquiry(String customerNo, String denomCode, String refId, BigDecimal amount) {
    DigiflazzClient.DigiInquiryResult r = digiflazzClient.inquiry(refId, denomCode, customerNo, amount);
    return new InquiryResult(emptyToNull(r.customerName()), r.price(), r.admin(),
            r.rc(), r.message(), r.desc());
}

@Override
public ProviderResponse payPostpaid(String customerNo, String denomCode, String refId) {
    DigiflazzClient.DigiTxResult r = digiflazzClient.payPostpaid(refId, denomCode, customerNo);
    return new ProviderResponse(mapStatus(r.status(), r.rc()), emptyToNull(r.refId()),
            emptyToNull(r.sn()), r.message(), r.price());
}
```

(Match the field name for the injected client to what `sendTransaction` already uses.)

- [ ] **Step 4: Run tests**

Run: `./mvnw test -Dtest=RealProviderAdapterPostpaidTest,RealProviderAdapterTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/satset/transaction/model/InquiryResult.java src/main/java/com/satset/transaction/client/ProviderPort.java src/main/java/com/satset/transaction/client/RealProviderAdapter.java src/test/java/com/satset/transaction/client/RealProviderAdapterPostpaidTest.java
git commit -m "feat(transaction): add postpaid inquiry/pay to provider port and adapter" -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 4: `DenomInfo` postpaid discriminators + `DenomRepository` projection + fix broken call sites

**Files:**
- Modify: `src/main/java/com/satset/shared/model/DenomInfo.java`
- Modify: `src/main/java/com/satset/catalog/repository/DenomRepository.java`
- Modify: `src/test/java/com/satset/transaction/service/topup/TransactionDomainServiceTest.java` (DenomInfo constructions near lines 59, 75, 106, 225)
- Modify: `src/test/java/com/satset/transaction/web/PurchaseFlowIntegrationTest.java` (DenomInfo construction near line 86)
- Test: `src/test/java/com/satset/shared/model/DenomInfoTest.java` (create)

**Interfaces:**
- Consumes: `com.satset.catalog.model.DenomType` (existing enum: `FIXED_DENOM`, `OPEN_AMOUNT`); `ProductDenoms` already has `denomType`, `minAmount`, `maxAmount`, `requiresInquiry` columns.
- Produces: `record DenomInfo(UUID id, String code, String name, String productName, BigDecimal price, BigDecimal adminFee, BigDecimal basePrice, boolean active, boolean deleted, boolean requiresInquiry, DenomType denomType, BigDecimal minAmount, BigDecimal maxAmount)` — the 4 new components are TRAILING, in this exact order. `total()`/`isAvailable()` unchanged.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/satset/shared/model/DenomInfoTest.java`:

```java
package com.satset.shared.model;

import com.satset.catalog.model.DenomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DenomInfoTest {

    @Test
    void carriesPostpaidDiscriminators() {
        DenomInfo d = new DenomInfo(UUID.randomUUID(), "gopay", "GoPay Saldo", "GoPay",
                BigDecimal.ZERO, new BigDecimal("1000"), BigDecimal.ZERO, true, false,
                true, DenomType.OPEN_AMOUNT, new BigDecimal("10000"), new BigDecimal("1000000"));

        assertThat(d.requiresInquiry()).isTrue();
        assertThat(d.denomType()).isEqualTo(DenomType.OPEN_AMOUNT);
        assertThat(d.minAmount()).isEqualByComparingTo("10000");
        assertThat(d.maxAmount()).isEqualByComparingTo("1000000");
        assertThat(d.isAvailable()).isTrue();
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -Dtest=DenomInfoTest`
Expected: COMPILE FAILURE — constructor has 9 parameters, not 13.

- [ ] **Step 3: Implementation**

1. In `DenomInfo.java`, append the 4 trailing record components: `boolean requiresInquiry, com.satset.catalog.model.DenomType denomType, BigDecimal minAmount, BigDecimal maxAmount`. Leave `total()` / `isAvailable()` untouched.
2. In `DenomRepository.java`, extend the JPQL constructor expression of `findDenomInfoById` — append `, d.requiresInquiry, d.denomType, d.minAmount, d.maxAmount` after `d.deleted` (before `FROM`), so the arg order matches the record.
3. Fix the 5 now-broken `new DenomInfo(...)` call sites — `TransactionDomainServiceTest` (~lines 59, 75, 106, 225) and `PurchaseFlowIntegrationTest` (~line 86). Each currently ends with `..., true, false)` (or similar `active, deleted` values); these are all prepaid fixtures, so append exactly: `, false, DenomType.FIXED_DENOM, null, null` and add `import com.satset.catalog.model.DenomType;` to both test files.

- [ ] **Step 4: Run the full suite**

Run: `./mvnw test`
Expected: PASS — all existing tests green (compile fixed), `DenomInfoTest` green.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/satset/shared/model/DenomInfo.java src/main/java/com/satset/catalog/repository/DenomRepository.java src/test/java/com/satset/shared/model/DenomInfoTest.java src/test/java/com/satset/transaction/service/topup/TransactionDomainServiceTest.java src/test/java/com/satset/transaction/web/PurchaseFlowIntegrationTest.java
git commit -m "feat(catalog): expose postpaid discriminators on DenomInfo projection" -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 5: `PostpaidService.inquiry` + amount rules + `InquiryDTO`

**Files:**
- Create: `src/main/java/com/satset/transaction/service/postpaid/PostpaidService.java`
- Create: `src/main/java/com/satset/transaction/dto/InquiryDTO.java`
- Test: `src/test/java/com/satset/transaction/service/postpaid/PostpaidServiceInquiryTest.java` (create)

**Interfaces:**
- Consumes: `DenomRepository.findDenomInfoById(UUID)`, `ProviderPort.inquiry(customerNo, denomCode, refId, amount)`, `RefNoGenerator.next()`, `InquiryResult` (Task 3), `DenomInfo` (Task 4), `BusinessException(code, message)` (checked), `SupplierException(code, message)` (runtime), `ResourceNotFoundException(resourceName, message)`, `@com.satset.shared.logging.LogContext`.
- Produces: `public InquiryDTO inquiry(UUID denomId, String customerNo, BigDecimal amount) throws BusinessException`; `record InquiryDTO(String customerName, BigDecimal bill, BigDecimal admin, BigDecimal markup, BigDecimal total, JsonNode desc)`; ctor `PostpaidService(DenomRepository, WalletGateway, ProviderPort, RefNoGenerator, TransactionRepository, TransactionDomainService)` (all six now — pay uses the rest in Task 9).
- Error codes produced: `NOT_POSTPAID`, `DENOM_UNAVAILABLE`, `AMOUNT_REQUIRED`, `AMOUNT_OUT_OF_RANGE`, `AMOUNT_NOT_ALLOWED`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/satset/transaction/service/postpaid/PostpaidServiceInquiryTest.java`:

```java
package com.satset.transaction.service.postpaid;

import com.satset.catalog.model.DenomType;
import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.shared.exception.SupplierException;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.dto.InquiryDTO;
import com.satset.transaction.model.InquiryResult;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.service.topup.RefNoGenerator;
import com.satset.transaction.service.topup.TransactionDomainService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostpaidServiceInquiryTest {

    private static final UUID DENOM_ID = UUID.randomUUID();

    private final DenomRepository denomRepository = mock(DenomRepository.class);
    private final WalletGateway walletGateway = mock(WalletGateway.class);
    private final ProviderPort providerPort = mock(ProviderPort.class);
    private final RefNoGenerator refNoGenerator = mock(RefNoGenerator.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final TransactionDomainService transactionDomainService = mock(TransactionDomainService.class);

    private final PostpaidService service = new PostpaidService(denomRepository, walletGateway,
            providerPort, refNoGenerator, transactionRepository, transactionDomainService);

    private static DenomInfo pascaDenom() { // FIXED_DENOM + requiresInquiry, markup 1500
        return new DenomInfo(DENOM_ID, "pln", "PLN Pascabayar", "PLN", BigDecimal.ZERO,
                new BigDecimal("1500"), BigDecimal.ZERO, true, false,
                true, DenomType.FIXED_DENOM, null, null);
    }

    private static DenomInfo emoneyDenom() { // OPEN_AMOUNT + requiresInquiry, 10k..1jt, markup 1000
        return new DenomInfo(DENOM_ID, "gopay", "GoPay Saldo", "GoPay", BigDecimal.ZERO,
                new BigDecimal("1000"), BigDecimal.ZERO, true, false,
                true, DenomType.OPEN_AMOUNT, new BigDecimal("10000"), new BigDecimal("1000000"));
    }

    @Test
    void fixedDenomInquiryReturnsBillWithMarkup() throws Exception {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(pascaDenom()));
        when(refNoGenerator.next()).thenReturn("TRX001");
        when(providerPort.inquiry("530000000001", "pln", "TRX001", null))
                .thenReturn(new InquiryResult("BUDI SANTOSO", new BigDecimal("145000"),
                        new BigDecimal("2500"), "00", "Sukses", null));

        InquiryDTO dto = service.inquiry(DENOM_ID, "530000000001", null);

        assertThat(dto.customerName()).isEqualTo("BUDI SANTOSO");
        assertThat(dto.bill()).isEqualByComparingTo("145000");
        assertThat(dto.admin()).isEqualByComparingTo("2500");
        assertThat(dto.markup()).isEqualByComparingTo("1500");
        assertThat(dto.total()).isEqualByComparingTo("149000");
        verifyNoInteractions(walletGateway, transactionRepository);
    }

    @Test
    void openAmountInquiryPassesAmountToProvider() throws Exception {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(emoneyDenom()));
        when(refNoGenerator.next()).thenReturn("TRX002");
        when(providerPort.inquiry("0812345678", "gopay", "TRX002", new BigDecimal("25000")))
                .thenReturn(new InquiryResult("BUDI", new BigDecimal("25000"),
                        new BigDecimal("1000"), "00", "Sukses", null));

        InquiryDTO dto = service.inquiry(DENOM_ID, "0812345678", new BigDecimal("25000"));

        assertThat(dto.total()).isEqualByComparingTo("27000"); // 25000 + 1000 + 1000
        verify(providerPort).inquiry("0812345678", "gopay", "TRX002", new BigDecimal("25000"));
    }

    @Test
    void unknownDenomThrowsResourceNotFound() {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "530000000001", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void nonPostpaidDenomIsRejected() {
        DenomInfo prepaid = new DenomInfo(DENOM_ID, "tsel10", "Telkomsel 10k", "Telkomsel",
                new BigDecimal("11000"), new BigDecimal("500"), new BigDecimal("10500"), true, false,
                false, DenomType.FIXED_DENOM, null, null);
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(prepaid));

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "530000000001", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("NOT_POSTPAID"));
    }

    @Test
    void unavailableDenomIsRejected() {
        DenomInfo inactive = new DenomInfo(DENOM_ID, "pln", "PLN Pascabayar", "PLN",
                BigDecimal.ZERO, new BigDecimal("1500"), BigDecimal.ZERO, false, false,
                true, DenomType.FIXED_DENOM, null, null);
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "530000000001", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("DENOM_UNAVAILABLE"));
    }

    @Test
    void fixedDenomWithAmountIsRejected() {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(pascaDenom()));

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "530000000001", new BigDecimal("50000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("AMOUNT_NOT_ALLOWED"));
        verifyNoInteractions(providerPort);
    }

    @Test
    void openAmountWithoutAmountIsRejected() {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(emoneyDenom()));

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "0812345678", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("AMOUNT_REQUIRED"));
    }

    @Test
    void openAmountOutOfRangeIsRejected() {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(emoneyDenom()));

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "0812345678", new BigDecimal("5000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("AMOUNT_OUT_OF_RANGE"));
        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "0812345678", new BigDecimal("2000000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("AMOUNT_OUT_OF_RANGE"));
        verifyNoInteractions(providerPort);
    }

    @Test
    void supplierFailureThrowsSupplierException() {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(pascaDenom()));
        when(refNoGenerator.next()).thenReturn("TRX003");
        when(providerPort.inquiry(any(), any(), any(), any()))
                .thenReturn(new InquiryResult(null, null, null, "14", "Nomor tidak ditemukan", null));

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "530000000001", null))
                .isInstanceOf(SupplierException.class)
                .hasMessage("Nomor tidak ditemukan");
    }
}
```

(If `TransactionRepository` lives in a different package, mirror the import used by `TransactionDomainServiceTest`.)

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -Dtest=PostpaidServiceInquiryTest`
Expected: COMPILE FAILURE — `PostpaidService` and `InquiryDTO` do not exist.

- [ ] **Step 3: Implementation**

Create `src/main/java/com/satset/transaction/dto/InquiryDTO.java`:

```java
package com.satset.transaction.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record InquiryDTO(String customerName, BigDecimal bill, BigDecimal admin,
        BigDecimal markup, BigDecimal total, JsonNode desc) {
}
```

Create `src/main/java/com/satset/transaction/service/postpaid/PostpaidService.java`:

```java
package com.satset.transaction.service.postpaid;

import com.satset.catalog.model.DenomType;
import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.shared.exception.SupplierException;
import com.satset.shared.logging.LogContext;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.dto.InquiryDTO;
import com.satset.transaction.model.InquiryResult;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.service.topup.RefNoGenerator;
import com.satset.transaction.service.topup.TransactionDomainService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@LogContext("Postpaid")
public class PostpaidService {

    private final DenomRepository denomRepository;
    private final WalletGateway walletGateway;
    private final ProviderPort providerPort;
    private final RefNoGenerator refNoGenerator;
    private final TransactionRepository transactionRepository;
    private final TransactionDomainService transactionDomainService;

    public PostpaidService(DenomRepository denomRepository, WalletGateway walletGateway,
            ProviderPort providerPort, RefNoGenerator refNoGenerator,
            TransactionRepository transactionRepository, TransactionDomainService transactionDomainService) {
        this.denomRepository = denomRepository;
        this.walletGateway = walletGateway;
        this.providerPort = providerPort;
        this.refNoGenerator = refNoGenerator;
        this.transactionRepository = transactionRepository;
        this.transactionDomainService = transactionDomainService;
    }

    public InquiryDTO inquiry(UUID denomId, String customerNo, BigDecimal amount) throws BusinessException {
        DenomInfo denom = loadPostpaidDenom(denomId);
        validateAmountRule(denom, amount);
        String ref = refNoGenerator.next();
        InquiryResult r = providerPort.inquiry(customerNo, denom.code(), ref, amount);
        if (!r.ok()) {
            throw new SupplierException(r.rc(), r.message());
        }
        BigDecimal total = r.bill().add(r.admin()).add(denom.adminFee());
        return new InquiryDTO(r.customerName(), r.bill(), r.admin(), denom.adminFee(), total, r.desc());
    }

    private DenomInfo loadPostpaidDenom(UUID denomId) throws BusinessException {
        DenomInfo denom = denomRepository.findDenomInfoById(denomId)
                .orElseThrow(() -> new ResourceNotFoundException("Denom", denomId.toString()));
        if (!denom.requiresInquiry()) {
            throw new BusinessException("NOT_POSTPAID", "Produk ini bukan produk pascabayar");
        }
        if (!denom.isAvailable()) {
            throw new BusinessException("DENOM_UNAVAILABLE", "Produk sedang tidak tersedia");
        }
        return denom;
    }

    private void validateAmountRule(DenomInfo denom, BigDecimal amount) throws BusinessException {
        if (denom.denomType() == DenomType.OPEN_AMOUNT) {
            if (amount == null) {
                throw new BusinessException("AMOUNT_REQUIRED", "Nominal wajib diisi untuk produk ini");
            }
            boolean belowMin = denom.minAmount() != null && amount.compareTo(denom.minAmount()) < 0;
            boolean aboveMax = denom.maxAmount() != null && amount.compareTo(denom.maxAmount()) > 0;
            if (belowMin || aboveMax) {
                throw new BusinessException("AMOUNT_OUT_OF_RANGE", "Nominal di luar batas yang diizinkan");
            }
        } else if (amount != null) {
            throw new BusinessException("AMOUNT_NOT_ALLOWED", "Produk ini membayar tagihan penuh tanpa nominal");
        }
    }
}
```

(Check `ResourceNotFoundException`'s two-arg constructor matches its use in `TransactionController` — `new ResourceNotFoundException("Store", "current user has no store")` — it does.)

- [ ] **Step 4: Run tests**

Run: `./mvnw test -Dtest=PostpaidServiceInquiryTest`
Expected: PASS (9 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/satset/transaction/service/postpaid/PostpaidService.java src/main/java/com/satset/transaction/dto/InquiryDTO.java src/test/java/com/satset/transaction/service/postpaid/PostpaidServiceInquiryTest.java
git commit -m "feat(transaction): add PostpaidService.inquiry with open-amount rules" -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 6: `POST /api/transactions/inquiry` endpoint

**Files:**
- Create: `src/main/java/com/satset/transaction/dto/InquiryRequest.java`
- Modify: `src/main/java/com/satset/transaction/web/TransactionController.java` (ctor currently `(TransactionDomainService, WalletGateway, UserDTO)`)
- Modify: `src/test/java/com/satset/transaction/web/TransactionControllerTest.java` and `src/test/java/com/satset/transaction/web/TransactionControllerSecurityTest.java` (constructor/bean wiring gains `PostpaidService`)
- Test: `src/test/java/com/satset/transaction/web/TransactionControllerPostpaidTest.java` (create)

**Interfaces:**
- Consumes: `PostpaidService.inquiry(UUID denomId, String customerNo, BigDecimal amount)` (Task 5), `SatsetConstants.PERM_PURCHASE` (`"CLIENT_purchase"`), `InquiryDTO`.
- Produces: `record InquiryRequest(@NotNull UUID denomId, @NotBlank String customerNo, BigDecimal amount)`; controller ctor becomes `(TransactionDomainService, WalletGateway, PostpaidService, UserDTO)`; `POST /api/transactions/inquiry` → 200 `InquiryDTO`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/satset/transaction/web/TransactionControllerPostpaidTest.java`:

```java
package com.satset.transaction.web;

import com.satset.shared.dto.UserDTO;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.dto.InquiryDTO;
import com.satset.transaction.dto.InquiryRequest;
import com.satset.transaction.service.postpaid.PostpaidService;
import com.satset.transaction.service.topup.TransactionDomainService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionControllerPostpaidTest {

    private static final UUID DENOM_ID = UUID.randomUUID();

    private final TransactionDomainService txService = mock(TransactionDomainService.class);
    private final WalletGateway walletGateway = mock(WalletGateway.class);
    private final PostpaidService postpaidService = mock(PostpaidService.class);
    private final UserDTO userDTO = mock(UserDTO.class);

    private final TransactionController controller =
            new TransactionController(txService, walletGateway, postpaidService, userDTO);

    @Test
    void inquiryDelegatesToPostpaidService() throws Exception {
        InquiryDTO dto = new InquiryDTO("BUDI SANTOSO", new BigDecimal("145000"),
                new BigDecimal("2500"), new BigDecimal("1500"), new BigDecimal("149000"), null);
        when(postpaidService.inquiry(DENOM_ID, "530000000001", null)).thenReturn(dto);

        var response = controller.inquiry(new InquiryRequest(DENOM_ID, "530000000001", null));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(dto);
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -Dtest=TransactionControllerPostpaidTest`
Expected: COMPILE FAILURE — 4-arg constructor, `inquiry` method, `InquiryRequest` do not exist.

- [ ] **Step 3: Implementation**

Create `src/main/java/com/satset/transaction/dto/InquiryRequest.java`:

```java
package com.satset.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record InquiryRequest(@NotNull UUID denomId, @NotBlank String customerNo, BigDecimal amount) {
}
```

In `TransactionController.java`: add field + ctor param `PostpaidService postpaidService` (order: `TransactionDomainService, WalletGateway, PostpaidService, UserDTO`), imports for `PostpaidService`, `InquiryRequest`, `InquiryDTO`, and:

```java
@PostMapping("/inquiry")
@PreAuthorize("hasRole('" + SatsetConstants.PERM_PURCHASE + "')")
public ResponseEntity<InquiryDTO> inquiry(@Valid @RequestBody InquiryRequest request)
        throws BusinessException {
    return ResponseEntity.ok(
            postpaidService.inquiry(request.denomId(), request.customerNo(), request.amount()));
}
```

Fix wiring in existing tests: any direct `new TransactionController(a, b, c)` in `TransactionControllerTest` gains a `mock(PostpaidService.class)` third argument; if `TransactionControllerSecurityTest` (or `PurchaseFlowIntegrationTest`) builds a Spring context with mocked beans, add `@MockitoBean private PostpaidService postpaidService;` alongside the existing mocked beans.

- [ ] **Step 4: Run tests**

Run: `./mvnw test -Dtest=TransactionControllerPostpaidTest,TransactionControllerTest,TransactionControllerSecurityTest,PurchaseFlowIntegrationTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/satset/transaction/dto/InquiryRequest.java src/main/java/com/satset/transaction/web/TransactionController.java src/test/java/com/satset/transaction/web/
git commit -m "feat(transaction): add POST /api/transactions/inquiry endpoint" -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Phase 2 — Backend pay

### Task 7: `Transactions.customerName` column

**Files:**
- Modify: `src/main/java/com/satset/transaction/model/Transactions.java` (`@Data` JPA entity)
- Test: `src/test/java/com/satset/transaction/model/TransactionsCustomerNameTest.java` (create)

**Interfaces:**
- Produces: `getCustomerName()` / `setCustomerName(String)` via Lombok on new field `@Column(name = "customer_name", length = 100) private String customerName;` (nullable).

- [ ] **Step 1: Write the failing test**

```java
package com.satset.transaction.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionsCustomerNameTest {

    @Test
    void customerNameIsNullableAndSettable() {
        Transactions tx = new Transactions();
        assertThat(tx.getCustomerName()).isNull();
        tx.setCustomerName("BUDI SANTOSO");
        assertThat(tx.getCustomerName()).isEqualTo("BUDI SANTOSO");
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -Dtest=TransactionsCustomerNameTest`
Expected: COMPILE FAILURE — `getCustomerName` does not exist.

- [ ] **Step 3: Implementation**

Add to `Transactions.java` next to the other nullable string columns:

```java
@Column(name = "customer_name", length = 100)
private String customerName;
```

**Prod migration note (ddl-auto=validate in prod; dev `update` auto-adds):** record in the commit body / release notes that prod needs, before deploy:

```sql
ALTER TABLE transactions ADD COLUMN customer_name varchar(100);
```

- [ ] **Step 4: Run tests**

Run: `./mvnw test -Dtest=TransactionsCustomerNameTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/satset/transaction/model/Transactions.java src/test/java/com/satset/transaction/model/TransactionsCustomerNameTest.java
git commit -m "feat(transaction): add nullable customerName to Transactions" -m "Prod (ddl-auto=validate) requires manual: ALTER TABLE transactions ADD COLUMN customer_name varchar(100);" -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 8: `BILL_CHANGED` → HTTP 409

**Files:**
- Modify: `src/main/java/com/satset/shared/exception/GlobalExceptionHandler.java` (lines 72–76, `handleBusinessException`)
- Test: `src/test/java/com/satset/shared/exception/GlobalExceptionHandlerTest.java` (create)

**Interfaces:**
- Consumes: `BusinessException.getErrorCode()` / `getErrorMessage()`; private `body(code, message, status)` helper already in the handler.
- Produces: `handleBusinessException` returns 409 CONFLICT when `errorCode` is `"BILL_CHANGED"`, 400 otherwise (signature unchanged: `ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex)`).

- [ ] **Step 1: Write the failing test**

```java
package com.satset.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void billChangedBusinessExceptionMapsTo409() {
        var response = handler.handleBusinessException(
                new BusinessException("BILL_CHANGED", "Tagihan berubah. Silakan cek ulang."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("BILL_CHANGED");
    }

    @Test
    void otherBusinessExceptionsStay400() {
        var response = handler.handleBusinessException(
                new BusinessException("AMOUNT_REQUIRED", "Nominal wajib diisi"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -Dtest=GlobalExceptionHandlerTest#billChangedBusinessExceptionMapsTo409`
Expected: FAIL — expected 409 CONFLICT but was 400 BAD_REQUEST.

- [ ] **Step 3: Implementation** — replace the body of `handleBusinessException`:

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
    log.error("Business exception: {} - {}", ex.getErrorCode(), ex.getErrorMessage());
    HttpStatus status = "BILL_CHANGED".equals(ex.getErrorCode())
            ? HttpStatus.CONFLICT
            : HttpStatus.BAD_REQUEST;
    return body(ex.getErrorCode(), ex.getErrorMessage(), status);
}
```

- [ ] **Step 4: Run tests**

Run: `./mvnw test -Dtest=GlobalExceptionHandlerTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/satset/shared/exception/GlobalExceptionHandler.java src/test/java/com/satset/shared/exception/GlobalExceptionHandlerTest.java
git commit -m "feat(shared): map BILL_CHANGED business error to HTTP 409" -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 9: `PostpaidService.pay` (money path)

**Files:**
- Modify: `src/main/java/com/satset/transaction/service/postpaid/PostpaidService.java`
- Test: `src/test/java/com/satset/transaction/service/postpaid/PostpaidServicePayTest.java` (create)

**Interfaces:**
- Consumes: `WalletGateway.deductBalance(String walletId, BigDecimal amount, UUID referenceId, String description)`; `TransactionRepository.existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(UUID, UUID, String, List<TransactionStatus>, LocalDateTime)`, `save`, `findById`; `TransactionDomainService.reconcileProviderResult(Transactions tx, ProviderResponse response, String walletId, DenomInfo denom)`; `ProviderPort.inquiry(...)` / `payPostpaid(...)`; `Transactions.setCustomerName` (Task 7); `TransactionDTO(id, refNo, storeId, targetNumber, denomName, productName, price, adminFee, total, status, providerRef, serialNumber, createdAt)`.
- Produces: `public TransactionDTO pay(UUID storeId, String walletId, UUID denomId, String customerNo, BigDecimal amount, BigDecimal expectedTotal) throws BusinessException`. Error codes: `DUPLICATE_TRANSACTION`, `BILL_CHANGED` (+ the Task 5 codes via the shared validators).

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/satset/transaction/service/postpaid/PostpaidServicePayTest.java` (same mock field setup and `pascaDenom()`/`emoneyDenom()` fixtures as `PostpaidServiceInquiryTest` — copy them in, do not share state between test classes):

```java
package com.satset.transaction.service.postpaid;

import com.satset.catalog.model.DenomType;
import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.SupplierException;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.client.ProviderResponse;
import com.satset.transaction.client.ProviderStatus;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.dto.TransactionDTO;
import com.satset.transaction.model.InquiryResult;
import com.satset.transaction.model.TransactionStatus;
import com.satset.transaction.model.Transactions;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.service.topup.RefNoGenerator;
import com.satset.transaction.service.topup.TransactionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PostpaidServicePayTest {

    private static final UUID DENOM_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID TX_ID = UUID.randomUUID();

    private final DenomRepository denomRepository = mock(DenomRepository.class);
    private final WalletGateway walletGateway = mock(WalletGateway.class);
    private final ProviderPort providerPort = mock(ProviderPort.class);
    private final RefNoGenerator refNoGenerator = mock(RefNoGenerator.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final TransactionDomainService transactionDomainService = mock(TransactionDomainService.class);

    private final PostpaidService service = new PostpaidService(denomRepository, walletGateway,
            providerPort, refNoGenerator, transactionRepository, transactionDomainService);

    private static DenomInfo pascaDenom() {
        return new DenomInfo(DENOM_ID, "pln", "PLN Pascabayar", "PLN", BigDecimal.ZERO,
                new BigDecimal("1500"), BigDecimal.ZERO, true, false,
                true, DenomType.FIXED_DENOM, null, null);
    }

    @BeforeEach
    void baseStubs() {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(pascaDenom()));
        when(transactionRepository.existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
                eq(STORE_ID), eq(DENOM_ID), eq("530000000001"), anyList(), any(LocalDateTime.class)))
                .thenReturn(false);
        when(refNoGenerator.next()).thenReturn("TRX010");
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(inv -> {
            Transactions t = inv.getArgument(0);
            t.setId(TX_ID);
            return t;
        });
    }

    private void stubFreshInquiry(String bill) {
        when(providerPort.inquiry("530000000001", "pln", "TRX010", null))
                .thenReturn(new InquiryResult("BUDI SANTOSO", new BigDecimal(bill),
                        new BigDecimal("2500"), "00", "Sukses", null));
    }

    @Test
    void successfulPayDeductsChargesAndReconciles() throws Exception {
        stubFreshInquiry("145000"); // total = 145000 + 2500 + 1500 = 149000
        ProviderResponse payResp = new ProviderResponse(ProviderStatus.SUCCESS, "DF123",
                "STRUK/PLN/1234567890", "Sukses", new BigDecimal("147500"));
        when(providerPort.payPostpaid("530000000001", "pln", "TRX010")).thenReturn(payResp);
        when(transactionRepository.findById(TX_ID)).thenAnswer(inv -> {
            Transactions settled = new Transactions();
            settled.setId(TX_ID);
            settled.setRefNo("TRX010");
            settled.setStatus(TransactionStatus.SUCCESS);
            settled.setTotal(new BigDecimal("149000"));
            return Optional.of(settled);
        });

        TransactionDTO dto = service.pay(STORE_ID, "wallet-1", DENOM_ID, "530000000001",
                null, new BigDecimal("149000"));

        ArgumentCaptor<Transactions> saved = ArgumentCaptor.forClass(Transactions.class);
        verify(transactionRepository).save(saved.capture());
        Transactions tx = saved.getValue();
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PROCESSING);
        assertThat(tx.getTargetNumber()).isEqualTo("530000000001");
        assertThat(tx.getPrice()).isEqualByComparingTo("145000");
        assertThat(tx.getAdminFee()).isEqualByComparingTo("4000"); // dfAdmin 2500 + markup 1500
        assertThat(tx.getTotal()).isEqualByComparingTo("149000");
        assertThat(tx.getRefNo()).isEqualTo("TRX010");
        assertThat(tx.getCustomerName()).isEqualTo("BUDI SANTOSO");

        verify(walletGateway).deductBalance(eq("wallet-1"), eq(new BigDecimal("149000")),
                eq(TX_ID), anyString());
        verify(providerPort).payPostpaid("530000000001", "pln", "TRX010");
        verify(transactionDomainService).reconcileProviderResult(same(tx), same(payResp),
                eq("wallet-1"), any(DenomInfo.class));
        assertThat(dto.status()).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test
    void billChangedSinceDisplayThrows409CodeBeforeAnyCharge() {
        stubFreshInquiry("150000"); // fresh total 154000 != expected 149000

        assertThatThrownBy(() -> service.pay(STORE_ID, "wallet-1", DENOM_ID, "530000000001",
                null, new BigDecimal("149000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("BILL_CHANGED"));

        verifyNoInteractions(walletGateway);
        verify(transactionRepository, never()).save(any());
        verify(providerPort, never()).payPostpaid(any(), any(), any());
    }

    @Test
    void inquiryFailureAbortsWithoutChargeOrRow() {
        when(providerPort.inquiry("530000000001", "pln", "TRX010", null))
                .thenReturn(new InquiryResult(null, null, null, "14", "Nomor tidak ditemukan", null));

        assertThatThrownBy(() -> service.pay(STORE_ID, "wallet-1", DENOM_ID, "530000000001",
                null, new BigDecimal("149000")))
                .isInstanceOf(SupplierException.class);

        verifyNoInteractions(walletGateway);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void doubleSubmitWithinOneMinuteIsBlocked() {
        when(transactionRepository.existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
                eq(STORE_ID), eq(DENOM_ID), eq("530000000001"), anyList(), any(LocalDateTime.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.pay(STORE_ID, "wallet-1", DENOM_ID, "530000000001",
                null, new BigDecimal("149000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("DUPLICATE_TRANSACTION"));

        verifyNoInteractions(providerPort, walletGateway);
    }

    @Test
    void pendingPayDelegatesToReconcileAndNeverRefundsItself() throws Exception {
        stubFreshInquiry("145000");
        ProviderResponse pending = new ProviderResponse(ProviderStatus.PENDING, null, null,
                "timeout", null);
        when(providerPort.payPostpaid("530000000001", "pln", "TRX010")).thenReturn(pending);
        when(transactionRepository.findById(TX_ID)).thenReturn(Optional.empty());

        service.pay(STORE_ID, "wallet-1", DENOM_ID, "530000000001", null, new BigDecimal("149000"));

        // refund on FAILED / hold on PENDING is reconcile's job (already covered by
        // TransactionDomainServiceTest) — the service must only delegate:
        verify(transactionDomainService).reconcileProviderResult(any(Transactions.class),
                same(pending), eq("wallet-1"), any(DenomInfo.class));
        verify(walletGateway, never()).refundBalance(any(), any(), any(), any());
    }
}
```

(FAILED-refund behavior rides the same delegation: `reconcileProviderResult` is the already-tested money-safe path, mirror `pendingPayDelegatesToReconcileAndNeverRefundsItself` with `ProviderStatus.FAILED` if extra confidence is wanted. Adjust `refundBalance` matcher arity to the actual `WalletGateway.refundBalance` signature.)

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -Dtest=PostpaidServicePayTest`
Expected: COMPILE FAILURE — `pay(...)` does not exist.

- [ ] **Step 3: Implementation** — add to `PostpaidService.java` (no method-level `@Transactional` wrapping the provider HTTP calls — mirror how `TransactionDomainService.createPurchase` structures its transaction boundaries):

```java
public TransactionDTO pay(UUID storeId, String walletId, UUID denomId, String customerNo,
        BigDecimal amount, BigDecimal expectedTotal) throws BusinessException {
    DenomInfo denom = loadPostpaidDenom(denomId);
    validateAmountRule(denom, amount);

    boolean duplicate = transactionRepository
            .existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
                    storeId, denomId, customerNo,
                    java.util.List.of(TransactionStatus.PENDING, TransactionStatus.PROCESSING,
                            TransactionStatus.SUCCESS),
                    java.time.LocalDateTime.now().minusMinutes(1));
    if (duplicate) {
        throw new BusinessException("DUPLICATE_TRANSACTION",
                "Transaksi serupa baru saja dibuat. Tunggu 1 menit sebelum mencoba lagi.");
    }

    String ref = refNoGenerator.next();
    InquiryResult inqNow = providerPort.inquiry(customerNo, denom.code(), ref, amount);
    if (!inqNow.ok()) {
        throw new SupplierException(inqNow.rc(), inqNow.message());
    }

    BigDecimal total = inqNow.bill().add(inqNow.admin()).add(denom.adminFee());
    if (expectedTotal.compareTo(total) != 0) {
        throw new BusinessException("BILL_CHANGED",
                "Tagihan berubah sejak pengecekan. Silakan cek tagihan ulang.");
    }

    Transactions tx = new Transactions();
    tx.setStoreId(storeId);
    tx.setWalletId(walletId);
    tx.setProductDenomId(denomId);
    tx.setDenomName(denom.name());
    tx.setProductName(denom.productName());
    tx.setTargetNumber(customerNo);
    tx.setPrice(inqNow.bill());
    tx.setAdminFee(inqNow.admin().add(denom.adminFee()));
    tx.setTotal(total);
    tx.setStatus(TransactionStatus.PROCESSING);
    tx.setRefNo(ref);
    tx.setCustomerName(inqNow.customerName());
    tx = transactionRepository.save(tx);

    walletGateway.deductBalance(walletId, total, tx.getId(),
            "Pembayaran " + denom.productName() + " " + customerNo);

    ProviderResponse payResp = providerPort.payPostpaid(customerNo, denom.code(), ref);
    transactionDomainService.reconcileProviderResult(tx, payResp, walletId, denom);

    Transactions settled = transactionRepository.findById(tx.getId()).orElse(tx);
    return toDTO(settled);
}

private TransactionDTO toDTO(Transactions tx) {
    return new TransactionDTO(tx.getId(), tx.getRefNo(), tx.getStoreId(), tx.getTargetNumber(),
            tx.getDenomName(), tx.getProductName(), tx.getPrice(), tx.getAdminFee(), tx.getTotal(),
            tx.getStatus(), tx.getProviderRef(), tx.getSerialNumber(), tx.getCreatedAt());
}
```

Add the new imports (`ProviderResponse`, `TransactionDTO`, `TransactionStatus`, `Transactions`).

**Flag (spec "out of scope" check):** while here, look at how the pending-settlement poll re-checks provider status for PROCESSING rows; if it re-POSTs prepaid-style only, note a follow-up ticket for `status-pasca` — do NOT implement it in this plan.

- [ ] **Step 4: Run tests**

Run: `./mvnw test -Dtest=PostpaidServicePayTest,PostpaidServiceInquiryTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/satset/transaction/service/postpaid/PostpaidService.java src/test/java/com/satset/transaction/service/postpaid/PostpaidServicePayTest.java
git commit -m "feat(transaction): add PostpaidService.pay with re-inquiry and mismatch guard" -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 10: `POST /api/transactions/pay` endpoint

**Files:**
- Create: `src/main/java/com/satset/transaction/dto/PayRequest.java`
- Modify: `src/main/java/com/satset/transaction/web/TransactionController.java`
- Test: `src/test/java/com/satset/transaction/web/TransactionControllerPostpaidTest.java` (extend)

**Interfaces:**
- Consumes: `PostpaidService.pay(UUID storeId, String walletId, UUID denomId, String customerNo, BigDecimal amount, BigDecimal expectedTotal)` (Task 9); controller's private `getStoreId()` / `getWalletId()` helpers (throw `ResourceNotFoundException` when absent).
- Produces: `record PayRequest(@NotNull UUID denomId, @NotBlank String customerNo, BigDecimal amount, @NotNull BigDecimal expectedTotal)`; `POST /api/transactions/pay` → 200 `TransactionDTO`.

- [ ] **Step 1: Write the failing test** (add to `TransactionControllerPostpaidTest`)

```java
@Test
void payDelegatesWithStoreAndWalletFromUser() throws Exception {
    UUID storeId = UUID.randomUUID();
    when(userDTO.getStoreId()).thenReturn(storeId);
    when(userDTO.getWalletId()).thenReturn("wallet-1");
    TransactionDTO dto = new TransactionDTO(UUID.randomUUID(), "TRX010", storeId,
            "530000000001", "PLN Pascabayar", "PLN", new java.math.BigDecimal("145000"),
            new java.math.BigDecimal("4000"), new java.math.BigDecimal("149000"),
            com.satset.transaction.model.TransactionStatus.SUCCESS, "DF123",
            "STRUK/PLN/1234567890", java.time.LocalDateTime.now());
    when(postpaidService.pay(storeId, "wallet-1", DENOM_ID, "530000000001",
            null, new java.math.BigDecimal("149000"))).thenReturn(dto);

    var response = controller.pay(new com.satset.transaction.dto.PayRequest(
            DENOM_ID, "530000000001", null, new java.math.BigDecimal("149000")));

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(dto);
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -Dtest=TransactionControllerPostpaidTest#payDelegatesWithStoreAndWalletFromUser`
Expected: COMPILE FAILURE — `PayRequest` and `controller.pay` do not exist.

- [ ] **Step 3: Implementation**

Create `src/main/java/com/satset/transaction/dto/PayRequest.java`:

```java
package com.satset.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PayRequest(@NotNull UUID denomId, @NotBlank String customerNo,
        BigDecimal amount, @NotNull BigDecimal expectedTotal) {
}
```

Add to `TransactionController.java`:

```java
@PostMapping("/pay")
@PreAuthorize("hasRole('" + SatsetConstants.PERM_PURCHASE + "')")
public ResponseEntity<TransactionDTO> pay(@Valid @RequestBody PayRequest request)
        throws BusinessException {
    return ResponseEntity.ok(postpaidService.pay(getStoreId(), getWalletId(),
            request.denomId(), request.customerNo(), request.amount(), request.expectedTotal()));
}
```

- [ ] **Step 4: Run tests**

Run: `./mvnw test`
Expected: PASS — full suite green at end of Phase 2.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/satset/transaction/dto/PayRequest.java src/main/java/com/satset/transaction/web/TransactionController.java src/test/java/com/satset/transaction/web/TransactionControllerPostpaidTest.java
git commit -m "feat(transaction): add POST /api/transactions/pay endpoint" -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Phase 3 — UI (reuse existing grid, branch flow — NOT a new page)

### Task 11: Purchase grid shows postpaid categories

**Files:**
- Modify: `src/main/java/com/satset/shared/web/PurchasePageController.java` (line 36 currently loads only `CategoryType.PREPAID`)
- Modify: `src/main/resources/templates/pages/purchase/index.html` (`loadCategories()` around line 287 fetches only `/api/categories/type/PREPAID`)
- Test: `src/test/java/com/satset/shared/web/PurchasePageControllerTest.java` (create)

**Interfaces:**
- Consumes: `CategoryDomainService.findByType(CategoryType)`, `CategoryType.PREPAID` / `CategoryType.POSTPAID`, existing `GET /api/categories/type/{type}` endpoint.
- Produces: `initialCategories` model attribute containing PREPAID then POSTPAID categories. (The denom JSON needs no change: `GET /api/categories/{cat}/products/{p}/denoms` returns the full `ProductDenoms` entity, which already serializes `requiresInquiry`, `denomType`, `minAmount`, `maxAmount` — verified in `ProductCatalogController`.)

- [ ] **Step 1: Write the failing test**

```java
package com.satset.shared.web;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.CategoryType;
import com.satset.catalog.service.category.CategoryDomainService;
import com.satset.shared.dto.UserDTO;
import com.satset.transaction.client.WalletGateway;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PurchasePageControllerTest {

    private final CategoryDomainService categoryService = mock(CategoryDomainService.class);
    private final WalletGateway walletGateway = mock(WalletGateway.class);
    private final UserDTO userDTO = mock(UserDTO.class);

    private final PurchasePageController controller =
            new PurchasePageController(categoryService, walletGateway, userDTO);

    @Test
    void purchasePageLoadsPrepaidAndPostpaidCategories() {
        Category pulsa = new Category();
        Category pasca = new Category();
        when(categoryService.findByType(CategoryType.PREPAID)).thenReturn(List.of(pulsa));
        when(categoryService.findByType(CategoryType.POSTPAID)).thenReturn(List.of(pasca));
        when(userDTO.getWalletId()).thenReturn(null);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.purchasePage(model);

        assertThat(view).isEqualTo("pages/purchase/index");
        @SuppressWarnings("unchecked")
        List<Category> categories = (List<Category>) model.getAttribute("initialCategories");
        assertThat(categories).containsExactly(pulsa, pasca);
    }
}
```

(If `Category` has no no-arg constructor, mirror how existing catalog tests build one — e.g. builder or all-args — keeping two distinct instances.)

- [ ] **Step 2: Run to verify failure**

Run: `./mvnw test -Dtest=PurchasePageControllerTest`
Expected: FAIL — `containsExactly(pulsa, pasca)` fails because only PREPAID is loaded.

- [ ] **Step 3: Implementation**

In `PurchasePageController.purchasePage`, replace the single `findByType` call:

```java
List<Category> categories = new java.util.ArrayList<>(categoryService.findByType(CategoryType.PREPAID));
categories.addAll(categoryService.findByType(CategoryType.POSTPAID));
```

In `pages/purchase/index.html`, replace the body of `loadCategories()` (the post-purchase refresh path):

```js
async loadCategories() {
    this.loadingCats = true;
    try {
        const [pre, post] = await Promise.all([
            fetch('/api/categories/type/PREPAID'),
            fetch('/api/categories/type/POSTPAID')
        ]);
        this.categories = [
            ...(pre.ok ? await pre.json() : []),
            ...(post.ok ? await post.json() : [])
        ];
    } catch (e) { Alpine.store('toast').error('Gagal memuat kategori'); }
    this.loadingCats = false;
},
```

- [ ] **Step 4: Run tests**

Run: `./mvnw test -Dtest=PurchasePageControllerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/satset/shared/web/PurchasePageController.java src/main/resources/templates/pages/purchase/index.html src/test/java/com/satset/shared/web/PurchasePageControllerTest.java
git commit -m "feat(purchase): include postpaid categories in purchase grid" -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 12: Postpaid branch in the purchase modal (inquiry → confirm → pay)

**Files:**
- Modify: `src/main/resources/templates/pages/purchase/index.html` only (no new page, no new template)

**Interfaces:**
- Consumes: `POST /api/transactions/inquiry` body `{denomId, customerNo, amount}` → `{customerName, bill, admin, markup, total, desc}` (Task 6); `POST /api/transactions/pay` body `{denomId, customerNo, amount, expectedTotal}` → `TransactionDTO` JSON (`id`, `status`, `total`, `providerRef`, `serialNumber`, ...) with 409 `{code:"BILL_CHANGED", message}` (Tasks 8, 10); denom objects `d` already carrying `requiresInquiry`, `denomType` (`'FIXED_DENOM'`/`'OPEN_AMOUNT'`), `minAmount`, `maxAmount`.
- Produces: branched `purchaseFlow()` — prepaid path byte-for-byte unchanged (renamed `payPrepaid()`).

- [ ] **Step 1: Re-read the current template**

Read `src/main/resources/templates/pages/purchase/index.html` end to end (~420 lines: modal at the top, result receipt view, `purchaseFlow()` script at the bottom) so the edits below land on the current markup, classes, and Alpine idioms. Key anchors: denom chip `@click="denom = d"` (~line 130), footer pay button `@click="pay()"` (~line 160), `number` input which calls `detect()`, state object (~line 247), `pay()` (~line 355), `resetForm()` (~line 389).

- [ ] **Step 2: Add postpaid state and getters to `purchaseFlow()`**

Add to the state object after `isSubmitting: false,`:

```js
// postpaid
inquiryData: null,
inquiring: false,
nominal: null,
```

Add these getters next to the existing `total` getter, and replace `total`/`canPay` as shown (existing prepaid expressions preserved in the else-branches):

```js
get isPostpaid() { return !!this.denom?.requiresInquiry; },
get isOpenAmount() { return this.denom?.denomType === 'OPEN_AMOUNT'; },
get nominalError() {
    if (!this.isPostpaid || !this.isOpenAmount || !this.nominal) return '';
    if (this.denom.minAmount != null && this.nominal < this.denom.minAmount)
        return 'Minimal ' + this.formatCurrency(this.denom.minAmount);
    if (this.denom.maxAmount != null && this.nominal > this.denom.maxAmount)
        return 'Maksimal ' + this.formatCurrency(this.denom.maxAmount);
    return '';
},
get canInquire() {
    return this.isPostpaid && this.number.length >= 5 && !this.inquiring
        && (!this.isOpenAmount || (this.nominal > 0 && !this.nominalError));
},
get total() {
    if (this.isPostpaid) return this.inquiryData ? this.inquiryData.total : 0;
    return this.denom ? this.denom.price + (this.denom.adminFee || 0) : 0;
},
get canPay() {
    if (this.isPostpaid) return !!this.inquiryData && !this.isInsufficient;
    return !!this.denom && this.number.length >= 10 && !this.isInsufficient;
},
```

- [ ] **Step 3: Add `checkBill()` and branch `pay()`**

Rename the existing `async pay()` to `async payPrepaid()` (body unchanged), then add:

```js
async checkBill() {
    if (!this.canInquire) return;
    this.inquiring = true; this.inquiryData = null;
    try {
        const res = await fetch('/api/transactions/inquiry', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                denomId: this.denom.id,
                customerNo: this.number,
                amount: this.isOpenAmount ? this.nominal : null
            })
        });
        const data = await res.json();
        if (res.ok) this.inquiryData = data;
        else Alpine.store('toast').error(data.message || 'Cek tagihan gagal');
    } catch (e) { Alpine.store('toast').error('Terjadi kesalahan jaringan'); }
    this.inquiring = false;
},

async pay() {
    if (!this.canPay) return;
    if (!this.isPostpaid) return this.payPrepaid();
    this.isSubmitting = true;
    try {
        const res = await fetch('/api/transactions/pay', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                denomId: this.denom.id,
                customerNo: this.number,
                amount: this.isOpenAmount ? this.nominal : null,
                expectedTotal: this.inquiryData.total
            })
        });
        const data = await res.json();
        if (res.ok) {
            this.closeModal();
            this.resultData = { ...data, transactionId: data.id };
            this.resultError = '';
            this.view = 'result';
            Alpine.store('toast').success('Pembayaran diproses');
            this.fetchBalance();
        } else if (res.status === 409) {
            Alpine.store('toast').error('Tagihan berubah. Menampilkan tagihan terbaru — silakan konfirmasi ulang.');
            this.inquiryData = null;
            await this.checkBill(); // re-inquiry; user confirms the fresh total
        } else if (res.status === 422) {
            Alpine.store('toast').error(data.message || 'Saldo tidak cukup');
            this.fetchBalance();
        } else {
            this.closeModal();
            this.resultData = { status: 'FAILED' };
            this.resultError = data.message || 'Terjadi kesalahan sistem';
            this.view = 'result';
        }
    } catch (e) {
        Alpine.store('toast').error('Terjadi kesalahan jaringan');
    } finally {
        this.isSubmitting = false;
    }
},
```

- [ ] **Step 4: Reset stale inquiry state on every upstream change**

- Denom chip button: `@click="denom = d"` → `@click="denom = d; inquiryData = null; nominal = null"`.
- `number` input: append `; inquiryData = null` to its existing `@input` handler (which calls `detect()`).
- In `openCat(...)` after `this.number = ''; this.operator = '';` add: `this.inquiryData = null; this.nominal = null;`
- In `selectProduct(p)` after `this.denom = null; this.denoms = [];` add: `this.inquiryData = null; this.nominal = null;`
- In `resetForm()` after `this.number = ''; this.operator = '';` add: `this.inquiryData = null; this.nominal = null;`

- [ ] **Step 5: Add the postpaid markup block**

Insert between the "Nominal" (denom chips) section and the footer (before the `<!-- Footer: balance + total + pay -->` div), matching the existing DaisyUI/Tailwind classes:

```html
<!-- Postpaid: nominal (open amount) + cek tagihan + bill card -->
<div x-show="isPostpaid" x-cloak class="mt-5 space-y-3">
    <div x-show="isOpenAmount">
        <label class="block text-sm font-medium mb-2">Nominal</label>
        <input type="number" x-model.number="nominal" @input="inquiryData = null"
            class="input input-bordered w-full tabular-nums" placeholder="Masukkan nominal"
            :min="denom?.minAmount" :max="denom?.maxAmount">
        <p x-show="nominalError" class="text-xs text-error mt-1" x-text="nominalError"></p>
    </div>
    <button @click="checkBill()" :disabled="!canInquire"
        class="btn btn-outline btn-primary w-full">
        <span x-show="!inquiring">Cek Tagihan</span>
        <span x-show="inquiring" class="loading loading-spinner loading-sm"></span>
    </button>
    <div x-show="inquiryData" x-cloak
        class="rounded-xl border border-base-300 bg-base-200/50 p-4 space-y-2 text-sm">
        <div class="flex justify-between gap-4">
            <span class="text-base-content/55">Nama Pelanggan</span>
            <span class="font-semibold" x-text="inquiryData?.customerName || '—'"></span>
        </div>
        <div class="flex justify-between gap-4">
            <span class="text-base-content/55">Tagihan</span>
            <span class="tabular-nums" x-text="formatCurrency(inquiryData?.bill)"></span>
        </div>
        <div class="flex justify-between gap-4">
            <span class="text-base-content/55">Biaya Admin</span>
            <span class="tabular-nums"
                x-text="formatCurrency((inquiryData?.admin || 0) + (inquiryData?.markup || 0))"></span>
        </div>
        <div class="flex justify-between gap-4 border-t border-dashed border-base-300 pt-2">
            <span class="font-medium">Total Bayar</span>
            <span class="font-bold tabular-nums text-primary"
                x-text="formatCurrency(inquiryData?.total)"></span>
        </div>
    </div>
</div>
```

- [ ] **Step 6: Verify manually** (no JS test harness exists in this repo; backend behavior is covered by Tasks 1–10)

1. `./mvnw test` — expected: full suite PASS (regression check).
2. `./mvnw spring-boot:run` with dev profile (DF `testing:true`), open `/purchase`.
3. Prepaid category → unchanged direct-pay flow works (regression).
4. Postpaid FIXED_DENOM (e.g. PLN pasca, DF test customer no): number input → Cek Tagihan → bill card shows nama/tagihan/admin/total → Bayar Sekarang → struk in the receipt view (`serialNumber` = struk).
5. Postpaid OPEN_AMOUNT: nominal below `minAmount` shows the client-side error and blocks Cek Tagihan; valid nominal → inquiry → total card → pay.
6. 409 path: temporarily hand-tamper `expectedTotal` via devtools (or re-use a changed test bill) → toast "Tagihan berubah" + automatic re-inquiry, no charge.
7. Changing number, nominal, denom, or product clears the bill card (no stale `expectedTotal`).

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/templates/pages/purchase/index.html
git commit -m "feat(purchase): branch postpaid inquiry/pay flow in purchase modal" -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Self-Review

**1. Spec coverage**
- Digiflazz `inq-pasca` / `pay-pasca` on shared `/transaction` endpoint, same sign, `testing` honored → Tasks 1–2. `status-pasca` explicitly out of scope; flag step included in Task 9 Step 3.
- Re-inquiry-at-pay decision (no inquiry persistence, fresh ref for both calls, DF ties pay to ref-bound inquiry) → Task 9 (`ref` used for both `inquiry` and `payPostpaid`).
- Open-amount branch: amount required + min/max for `OPEN_AMOUNT`, null-only for `FIXED_DENOM`, amount sent to DF only in `inq-pasca`, never in `pay-pasca` → Tasks 1 (`@JsonInclude(NON_NULL)` + `$.amount` assertions), 2 (`doesNotExist` always), 5 (`validateAmountRule` + rejection tests), 9 (shared validator), 12 (client-side min/max).
- `ProviderPort`/`RealProviderAdapter`/`InquiryResult` → Task 3 (spec's early port sketch omitted `amount`; the spec's own "Product types" section supersedes it — 4-arg signature used consistently).
- `total = bill + dfAdmin + denom.adminFee` markup and `costPrice`/margin via existing reconcile → Tasks 5, 9.
- Mismatch guard 409 `BILL_CHANGED` → Tasks 8 (handler branch, verified against actual `GlobalExceptionHandler`), 9 (guard before save/deduct), 12 (re-inquiry UX).
- Double-submit guard, deduct, reconcile reuse (SUCCESS/PENDING/FAILED money-safe paths) → Task 9.
- `Transactions.customerName` only data change + prod `ALTER TABLE` note (`ddl-auto=validate`) → Task 7.
- Endpoints with `PERM_PURCHASE` → Tasks 6, 10.
- UI: reuse existing grid, branch after denom selection, denom payload already exposes discriminators (entity serialized directly — no payload task needed, verified in `ProductCatalogController`), plus the gap the spec implies: purchase page/JS only loaded PREPAID categories → Tasks 11–12.
- Error handling: `SupplierException` → 502 (verified existing), inquiry failure = zero side effects (Task 5/9 tests), HTTP pay failure → PENDING → PROCESSING hold (Task 3 mapping test + Task 9 delegation), no raw `e.getMessage()` to client (existing handler, unchanged).

**2. Placeholder scan** — no TBD/TODO/"similar to Task N" steps; every code step carries full code. Two deliberate, bounded adapt-notes remain: Task 1/3 "mirror the file's actual private field/ctor names" (the summaries of those internals are trusted, exact identifiers live in the file) and Task 11's `Category` construction note — each states exactly what to mirror and where.

**3. Type consistency** — `DenomInfo` 13-component order (…, `active`, `deleted`, `requiresInquiry`, `denomType`, `minAmount`, `maxAmount`) identical in Task 4 record, JPQL, and all fixtures in Tasks 5/9; `ProviderPort.inquiry(String customerNo, String denomCode, String refId, BigDecimal amount)` matches adapter (Task 3) and service calls (5/9); `DigiflazzClient.inquiry(String refId, String buyerSkuCode, String customerNo, BigDecimal amount)` argument order consistent between adapter delegation and client tests; `InquiryDTO(customerName, bill, admin, markup, total, desc)` matches Tasks 5/6/12 (UI reads `.bill/.admin/.markup/.total`); `pay(...)` 6-arg order identical in Tasks 9 and 10; `PascaRequest` snake_case fields match the jsonPath assertions; `TransactionDTO` 13-field ctor matches the existing record (verified) in Tasks 9/10; UI maps `data.id → transactionId` for the receipt view.

---

## Execution Handoff

Plan complete. Save to `docs/superpowers/plans/2026-07-21-pascabayar.md`. Two execution options: **1. Subagent-Driven (recommended)** — dispatch a fresh subagent per task via superpowers:subagent-driven-development, review between tasks; **2. Inline Execution** — superpowers:executing-plans, batch execution with checkpoints.

---
