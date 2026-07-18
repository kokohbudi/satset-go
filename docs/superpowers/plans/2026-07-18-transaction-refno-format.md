# Transaction ref_no (YYYYMMDDXXXXX) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the UUID sent to Digiflazz as `ref_id` with a human-readable `YYYYMMDDXXXXX` (date + daily counter), persisted on the transaction and reused verbatim on reconcile.

**Architecture:** A new `ref_counter(day, seq)` table gives an atomic per-day counter via a Postgres `INSERT ... ON CONFLICT ... RETURNING` UPSERT run in its own short transaction (`REQUIRES_NEW`, so the counter row lock is released before the slow provider HTTP call). `RefNoGenerator` formats `day + %05d`. `Transactions` gains a nullable unique `ref_no` column, generated once at purchase creation and read back on reconcile; old rows (null) fall back to the UUID.

**Tech Stack:** Spring Boot 4, Java 25, Hibernate JPA, Spring JDBC (`JdbcTemplate`), PostgreSQL, JUnit 5 + AssertJ + Mockito.

## Global Constraints

- Spring Boot 4.0.1, Java 25, Maven. Build/test module: `satset-core`.
- DB tests run against the **shared dev Postgres** via `@SpringBootTest` (NOT Testcontainers) — mirror `AccountingRepositoryTest`.
- New `@Entity` + repo live under `com.satset.transaction.model` / `com.satset.transaction.repository` — both packages are **already** registered in `CoreDataSourceConfig`, so that file needs **no change**.
- Date bucket timezone: `Asia/Jakarta` (WIB). Counter: 5-digit zero-pad, resets per WIB day.
- `ref_no` is nullable (old rows stay null) and unique.
- The wallet mutation `referenceId` stays the transaction UUID — do NOT touch it.
- TDD: write the failing test first, watch it fail, implement minimally, watch it pass, commit.
- Run a single test class with: `./mvnw -pl satset-core test -Dtest=<ClassName>`

---

### Task 1: RefNoGenerator + ref_counter table

**Files:**
- Create: `satset-core/src/main/java/com/satset/transaction/model/RefCounter.java`
- Create: `satset-core/src/main/java/com/satset/transaction/service/topup/RefNoGenerator.java`
- Test: `satset-core/src/test/java/com/satset/transaction/service/topup/RefNoGeneratorFormatTest.java`
- Test: `satset-core/src/test/java/com/satset/transaction/service/topup/RefNoGeneratorIntegrationTest.java`

**Interfaces:**
- Produces:
  - `RefNoGenerator.format(LocalDate day, long seq) -> String` (static, pure) — `"YYYYMMDD" + "%05d"`.
  - `RefNoGenerator.nextSeq(LocalDate day) -> long` — atomic UPSERT, `@Transactional(REQUIRES_NEW)`.
  - `RefNoGenerator.next() -> String` — `format(today WIB, nextSeq(today WIB))`.

- [ ] **Step 1: Write the failing pure-format test**

`RefNoGeneratorFormatTest.java`:
```java
package com.satset.transaction.service.topup;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class RefNoGeneratorFormatTest {

    @Test
    void format_padsCounterToFiveDigits() {
        assertThat(RefNoGenerator.format(LocalDate.of(2026, 7, 18), 1))
                .isEqualTo("2026071800001");
    }

    @Test
    void format_atDailyCap() {
        assertThat(RefNoGenerator.format(LocalDate.of(2026, 7, 18), 99999))
                .isEqualTo("2026071899999");
    }

    @Test
    void format_beyondCapGrowsWidthDoesNotBreak() {
        // %05d is a floor, not a ceiling: seq 100000 renders as 6 digits, no crash.
        assertThat(RefNoGenerator.format(LocalDate.of(2026, 7, 18), 100000))
                .isEqualTo("20260718100000");
    }
}
```

- [ ] **Step 2: Run it, verify it fails to compile**

Run: `./mvnw -pl satset-core test -Dtest=RefNoGeneratorFormatTest`
Expected: compile FAIL — `RefNoGenerator` does not exist.

- [ ] **Step 3: Create the `RefCounter` entity**

`RefCounter.java`:
```java
package com.satset.transaction.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Per-day counter backing the human-readable transaction ref_no. One row per WIB
 * day; {@code seq} is bumped atomically via an UPSERT in {@code RefNoGenerator}.
 * The entity exists only so Hibernate ddl-auto creates the table — increments go
 * through raw SQL, not JPA.
 */
@Entity
@Table(name = "ref_counter")
public class RefCounter {

    @Id
    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Column(name = "seq", nullable = false)
    private Long seq;
}
```

- [ ] **Step 4: Create `RefNoGenerator`**

`RefNoGenerator.java`:
```java
package com.satset.transaction.service.topup;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Generates the outward-facing transaction ref_no: {@code YYYYMMDD} (WIB day) +
 * a 5-digit daily counter. Internal DB access only — no {@code @LogContext}
 * (that rule is for outbound supplier calls, not own-DB writes).
 */
@Service
public class RefNoGenerator {

    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");
    private final JdbcTemplate jdbc;

    public RefNoGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Full ref_no for today (WIB). */
    public String next() {
        LocalDate day = LocalDate.now(WIB);
        return format(day, nextSeq(day));
    }

    /**
     * Atomic per-day counter bump. REQUIRES_NEW so the counter-row lock is held
     * only for this UPSERT and released on its own commit — NOT across the caller's
     * provider HTTP call. Gaps (when the caller's outer tx later rolls back) are
     * acceptable; a duplicate seq would corrupt Digiflazz idempotency, a gap won't.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long nextSeq(LocalDate day) {
        Long seq = jdbc.queryForObject("""
                INSERT INTO ref_counter(day, seq) VALUES (?, 1)
                ON CONFLICT (day) DO UPDATE SET seq = ref_counter.seq + 1
                RETURNING seq
                """, Long.class, Date.valueOf(day));
        return seq;
    }

    static String format(LocalDate day, long seq) {
        return day.format(DateTimeFormatter.BASIC_ISO_DATE) + String.format("%05d", seq);
    }
}
```

- [ ] **Step 5: Run the format test, verify it passes**

Run: `./mvnw -pl satset-core test -Dtest=RefNoGeneratorFormatTest`
Expected: PASS (3 tests).

- [ ] **Step 6: Write the failing atomicity integration test**

`RefNoGeneratorIntegrationTest.java`:
```java
package com.satset.transaction.service.topup;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against shared dev Postgres. Uses a far-past day (1999-01-01) so the real
 * "today" counter is never touched or reset, then deletes that row after.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class RefNoGeneratorIntegrationTest {

    private static final LocalDate TEST_DAY = LocalDate.of(1999, 1, 1);

    @Autowired RefNoGenerator generator;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM ref_counter WHERE day = ?", Date.valueOf(TEST_DAY));
    }

    @Test
    void nextSeq_isUniqueUnderConcurrency() throws InterruptedException {
        int n = 50;
        Set<Long> seqs = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    seqs.add(generator.nextSeq(TEST_DAY));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(seqs).hasSize(n);                 // no duplicates
        assertThat(seqs).containsExactlyInAnyOrderElementsOf(
                java.util.stream.LongStream.rangeClosed(1, n).boxed().toList()); // 1..n, no gaps
    }

    @Test
    void next_prefixesTodayInWib() {
        String expectedPrefix = LocalDate.now(java.time.ZoneId.of("Asia/Jakarta"))
                .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        assertThat(generator.next()).startsWith(expectedPrefix).hasSize(13);
        // note: this bumps today's real counter by 1 — harmless, gaps are expected.
    }
}
```

- [ ] **Step 7: Run it, verify it passes**

Run: `./mvnw -pl satset-core test -Dtest=RefNoGeneratorIntegrationTest`
Expected: PASS (2 tests). Hibernate ddl-auto creates `ref_counter` on context start.
If it fails with "relation ref_counter does not exist", the entity is not being scanned — confirm `RefCounter` sits in `com.satset.transaction.model`.

- [ ] **Step 8: Commit**

```bash
git add satset-core/src/main/java/com/satset/transaction/model/RefCounter.java \
        satset-core/src/main/java/com/satset/transaction/service/topup/RefNoGenerator.java \
        satset-core/src/test/java/com/satset/transaction/service/topup/RefNoGeneratorFormatTest.java \
        satset-core/src/test/java/com/satset/transaction/service/topup/RefNoGeneratorIntegrationTest.java
git commit -m "feat(transaction): RefNoGenerator + ref_counter atomic daily counter"
```

---

### Task 2: `ref_no` column + DTO field

**Files:**
- Modify: `satset-core/src/main/java/com/satset/transaction/model/Transactions.java` (add field after `serialNumber`, ~line 69)
- Modify: `satset-core/src/main/java/com/satset/transaction/dto/TransactionDTO.java` (add `refNo` after `id`)

**Interfaces:**
- Produces: `Transactions.getRefNo()/setRefNo(String)`; `TransactionDTO.refNo()` (2nd component, after `id`).

- [ ] **Step 1: Add the entity column**

In `Transactions.java`, after the `serialNumber` field (line 68-69), add:
```java
    @Column(name = "ref_no", unique = true, length = 20)
    private String refNo;
```

- [ ] **Step 2: Add the DTO component**

In `TransactionDTO.java`, insert `refNo` as the second component:
```java
public record TransactionDTO(
        UUID id,
        String refNo,
        UUID storeId,
        String targetNumber,
        String denomName,
        String productName,
        BigDecimal price,
        BigDecimal adminFee,
        BigDecimal total,
        TransactionStatus status,
        String providerRef,
        String serialNumber,
        LocalDateTime createdAt) {
}
```

- [ ] **Step 3: Compile (expected to fail — `toDTO` now has wrong arg count)**

Run: `./mvnw -pl satset-core -q compile`
Expected: FAIL — `TransactionDomainService.toDTO` constructor arity mismatch. This is fixed in Task 3. (If you are running tasks out of order, jump to Task 3 Step 1.)

- [ ] **Step 4: Commit after Task 3 compiles** (this column change is committed together with Task 3's `toDTO` fix — no standalone commit).

---

### Task 3: Generate ref_no on purchase + send it to the provider

**Files:**
- Modify: `satset-core/src/main/java/com/satset/transaction/service/topup/TransactionDomainService.java`
- Test: `satset-core/src/test/java/com/satset/transaction/web/PurchaseFlowIntegrationTest.java` (add one assertion — see Step 1)

**Interfaces:**
- Consumes: `RefNoGenerator.next()` (Task 1); `Transactions.setRefNo` / `TransactionDTO(refNo)` (Task 2).

- [ ] **Step 1: Write the failing test — provider receives the ref_no, DTO exposes it**

Open `PurchaseFlowIntegrationTest.java` and locate the successful-purchase test that stubs `providerService.sendTransaction(...)` and asserts on the returned `TransactionDTO`. This test uses `@MockitoBean ProviderPort providerService`. Add, after the purchase call returns `dto`:
```java
        // ref_no: YYYYMMDD + 5-digit counter, and the SAME value went to the provider
        assertThat(dto.refNo()).matches("\\d{8}\\d{5,}");
        org.mockito.ArgumentCaptor<String> refIdCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(providerService).sendTransaction(
                org.mockito.ArgumentMatchers.eq(targetNumber),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                refIdCaptor.capture());
        assertThat(refIdCaptor.getValue()).isEqualTo(dto.refNo());
        assertThat(refIdCaptor.getValue()).doesNotContain("-"); // not a UUID
```
(If the test already has a `verify(providerService).sendTransaction(...)` with a positional `refId` arg, replace that arg with the captor as above so the two assertions don't double-verify.)

- [ ] **Step 2: Run it, verify it fails**

Run: `./mvnw -pl satset-core test -Dtest=PurchaseFlowIntegrationTest`
Expected: FAIL — `refIdCaptor.getValue()` is the UUID (`transaction.getId().toString()`), contains `-`, and `dto.refNo()` is null.

- [ ] **Step 3: Inject `RefNoGenerator`**

In `TransactionDomainService.java`, add the field + constructor param:
```java
        private final ProviderPort providerService;
        private final RefNoGenerator refNoGenerator;

        public TransactionDomainService(TransactionRepository transactionRepository,
                        DenomRepository denomRepository,
                        WalletGateway balanceService,
                        ProviderPort providerService,
                        RefNoGenerator refNoGenerator) {
                this.transactionRepository = transactionRepository;
                this.denomRepository = denomRepository;
                this.balanceService = balanceService;
                this.providerService = providerService;
                this.refNoGenerator = refNoGenerator;
        }
```

- [ ] **Step 4: Set ref_no before the first save, send it to the provider**

In `createPurchase`, at the block that sets `PENDING` then saves (currently lines 85-86):
```java
                transaction.setStatus(TransactionStatus.PENDING);
                transaction.setRefNo(refNoGenerator.next());
                transaction = transactionRepository.save(transaction);
```
Then change the provider send (currently line 101-102) from `transaction.getId().toString()` to:
```java
                ProviderResponse response = providerService.sendTransaction(
                                targetNumber, denom.code(), total, transaction.getRefNo());
```

- [ ] **Step 5: Map `refNo` in `toDTO`**

In `toDTO` (currently line 166-180), add `tx.getRefNo()` as the second argument:
```java
        private TransactionDTO toDTO(Transactions tx) {
                return new TransactionDTO(
                                tx.getId(),
                                tx.getRefNo(),
                                tx.getStoreId(),
                                tx.getTargetNumber(),
                                tx.getDenomName(),
                                tx.getProductName(),
                                tx.getPrice(),
                                tx.getAdminFee(),
                                tx.getTotal(),
                                tx.getStatus(),
                                tx.getProviderRef(),
                                tx.getSerialNumber(),
                                tx.getCreatedAt());
        }
```

- [ ] **Step 6: Run the test, verify it passes**

Run: `./mvnw -pl satset-core test -Dtest=PurchaseFlowIntegrationTest`
Expected: PASS.

- [ ] **Step 7: Commit** (includes Task 2's column + DTO change)

```bash
git add satset-core/src/main/java/com/satset/transaction/model/Transactions.java \
        satset-core/src/main/java/com/satset/transaction/dto/TransactionDTO.java \
        satset-core/src/main/java/com/satset/transaction/service/topup/TransactionDomainService.java \
        satset-core/src/test/java/com/satset/transaction/web/PurchaseFlowIntegrationTest.java
git commit -m "feat(transaction): generate ref_no on purchase, send as Digiflazz ref_id"
```

---

### Task 4: Reconcile re-POSTs the stored ref_no

**Files:**
- Modify: `satset-core/src/main/java/com/satset/transaction/service/reconcile/TransactionReconcileService.java` (line 93-94)
- Test: `satset-core/src/test/java/com/satset/transaction/service/reconcile/TransactionReconcileServiceTest.java`

**Interfaces:**
- Consumes: `Transactions.getRefNo()` (Task 2).

- [ ] **Step 1: Write the failing test — reconcile sends the stored ref_no**

Add to `TransactionReconcileServiceTest.java`:
```java
    @Test
    void reconcile_usesStoredRefNo_notUuid() {
        Transactions tx = stale();
        tx.setRefNo("2026071800042");
        when(txRepo.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(), any()))
                .thenReturn(List.of(tx));
        when(txRepo.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(denomRepo.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        ProviderResponse resp = new ProviderResponse(ProviderStatus.SUCCESS, "REF", "SN", "Sukses", new BigDecimal("24500"));
        when(provider.sendTransaction("0878", "xld25", new BigDecimal("25000.00"), "2026071800042"))
                .thenReturn(resp);

        reconcile.reconcileStalePending();

        verify(provider).sendTransaction("0878", "xld25", new BigDecimal("25000.00"), "2026071800042");
    }
```
The three existing tests set no `refNo` (null) and still assert on `tx.getId().toString()` — they exercise the fallback and must keep passing unchanged.

- [ ] **Step 2: Run it, verify the new test fails**

Run: `./mvnw -pl satset-core test -Dtest=TransactionReconcileServiceTest`
Expected: `reconcile_usesStoredRefNo_notUuid` FAILS (service still sends `tx.getId().toString()`); the other 3 PASS.

- [ ] **Step 3: Send the stored ref_no with a UUID fallback**

In `TransactionReconcileService.settleOne` (line 93-94), change:
```java
        ProviderResponse resp = provider.sendTransaction(
                tx.getTargetNumber(), denom.code(), tx.getTotal(), refIdFor(tx));
```
and add a private helper below `settleOne`:
```java
    /** Pre-ref_no rows have a null ref_no; fall back to the UUID so their re-POST still matches Digiflazz. */
    private static String refIdFor(Transactions tx) {
        return tx.getRefNo() != null ? tx.getRefNo() : tx.getId().toString();
    }
```

- [ ] **Step 4: Run the test class, verify all pass**

Run: `./mvnw -pl satset-core test -Dtest=TransactionReconcileServiceTest`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/java/com/satset/transaction/service/reconcile/TransactionReconcileService.java \
        satset-core/src/test/java/com/satset/transaction/service/reconcile/TransactionReconcileServiceTest.java
git commit -m "feat(transaction): reconcile re-POSTs stored ref_no (UUID fallback for old rows)"
```

---

### Task 5: Show ref_no as the invoice number in the UI

**Files:**
- Modify: `satset-core/src/main/resources/templates/pages/transactions/index.html` (line ~308)

**Interfaces:**
- Consumes: `TransactionDTO.refNo()` → serialized as `tx.refNo` in the JSON the page consumes.

- [ ] **Step 1: Swap the detail-modal TrxID to ref_no with a fallback**

At line 308 (`TrxID: <span x-text="selectedTx?.id"></span>`), change the binding to prefer `refNo`, falling back to the UUID for old rows:
```html
                        TrxID: <span x-text="selectedTx?.refNo || selectedTx?.id"></span>
```

- [ ] **Step 2: Verify by eye**

Run the app and open a transaction's detail modal (or re-run `./mvnw -pl satset-core test -Dtest=PurchaseFlowIntegrationTest` to confirm nothing broke). New transactions show `2026071800001`; old ones still show the UUID.
Expected: no template parse error on startup; modal renders the ref_no.

- [ ] **Step 3: Commit**

```bash
git add satset-core/src/main/resources/templates/pages/transactions/index.html
git commit -m "feat(transaction-ui): show ref_no as invoice in detail modal (UUID fallback)"
```

---

### Task 6: Full regression + prod rollout note

- [ ] **Step 1: Run the whole module test suite**

Run: `./mvnw -pl satset-core test`
Expected: BUILD SUCCESS, no regressions.

- [ ] **Step 2: Record the prod DDL (ddl-auto=validate won't create these)**

Prod runs `ddl-auto=validate`, so the schema must be applied manually before deploy:
```sql
CREATE TABLE ref_counter (day DATE PRIMARY KEY, seq BIGINT NOT NULL);
ALTER TABLE transactions ADD COLUMN ref_no VARCHAR(20) UNIQUE;
```
Add this to the deploy checklist / release notes (same place the accounting `cost_price`/`margin` ALTERs were recorded).

- [ ] **Step 3: Update graph + tasks**

```bash
graphify update .
```
Mark the ref_no task done in `Tasks.md` and the Google Tasks list.

---

## Notes / open items

- Daily cap is 99,999 tx/day; `%05d` degrades gracefully to 6 digits past that rather than crashing. Widen the pad and revisit only if real daily volume approaches the cap.
- The counter increments in `REQUIRES_NEW`, so a rolled-back purchase leaves a gap in the day's sequence. Gaps are intended — uniqueness matters, contiguity does not.
