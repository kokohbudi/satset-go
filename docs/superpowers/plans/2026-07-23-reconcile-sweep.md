# Reconcile Stuck-Tx Sweep Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reinstate a core-side `@Scheduled` sweep that re-polls Digiflazz for topups stuck in `PROCESSING` and settles them via the existing `reconcileProviderResult`.

**Architecture:** One `@Scheduled` service (`TransactionReconcileService`) scans stale `PROCESSING` rows, settles each in its own transaction via `TransactionTemplate`, re-polls DF through the mockable `ProviderPort` (idempotent by ref_id = doubles as status check), and gives up past a max-age cutoff with an ALERT log. A derived repo finder supplies the batch; `@EnableScheduling` + `application.yml` wire it on.

**Tech Stack:** Spring Boot 4, Spring Scheduling, Spring Data JPA (derived query), Lombok, JUnit 5 + Mockito.

## Global Constraints

- Java 25, Spring Boot 4.0.1, Maven module `satset-core`.
- Entity == domain model; `Transactions` is `@Data` (Lombok getters/setters) with `@Version` optimistic lock and `@CreatedDate createdAt`.
- Outbound goes through a `@LogContext`-annotated service; the raw HTTP is logged centrally by `ProviderHttpConfig` → `logs/supplier/`. This service's business logs route to `logs/Reconcile/`.
- Provider access via `ProviderPort` (mockable seam) — never `DigiflazzClient` directly from the service.
- TDD: red → green. Frequent commits.
- Scope is `PROCESSING`-only (see spec `docs/superpowers/specs/2026-07-23-reconcile-sweep-design.md`).

---

### Task 1: Repo finder for stale PROCESSING rows

**Files:**
- Modify: `satset-core/src/main/java/com/satset/transaction/repository/TransactionRepository.java`

**Interfaces:**
- Produces: `List<Transactions> findByStatusAndCreatedAtBefore(TransactionStatus status, LocalDateTime cutoff, Pageable pageable)` — Spring Data derived query; oldest-first + batch cap supplied by the caller's `Pageable`.

**Note (ponytail):** No dedicated repo test. This is a Spring-Data-generated derived query, validated at context startup (a name typo fails bean creation, caught by any `@SpringBootTest` context load — e.g. `PurchaseFlowIntegrationTest`). The service unit test (Task 2) mocks this finder, covering our usage. No `@DataJpaTest` precedent in this codebase (repo tests use `@SpringBootTest` against shared dev Postgres); booting the full app to assert Spring's own query engine is disproportionate. Add a real test only if this ever becomes a hand-written `@Query`.

- [ ] **Step 1: Add the finder method**

In `TransactionRepository` (after the `existsBy...` method, imports `List`, `LocalDateTime`, `Pageable`, `TransactionStatus`, `Transactions` already present):

```java
    /**
     * Stale rows for the reconcile sweep: a given status older than {@code cutoff},
     * ordered + capped by the caller's {@link Pageable} (oldest first, batch size).
     */
    List<Transactions> findByStatusAndCreatedAtBefore(
            TransactionStatus status, LocalDateTime cutoff, Pageable pageable);
```

- [ ] **Step 2: Compile to verify the derived query parses**

Run: `mvn -q -pl satset-core -am compile`
Expected: BUILD SUCCESS (method name parses; no impl needed).

- [ ] **Step 3: Commit**

```bash
git add satset-core/src/main/java/com/satset/transaction/repository/TransactionRepository.java
git commit -m "feat(transaction): add findByStatusAndCreatedAtBefore for reconcile sweep"
```

---

### Task 2: TransactionReconcileService + unit tests

**Files:**
- Create: `satset-core/src/main/java/com/satset/transaction/service/reconcile/TransactionReconcileService.java`
- Create: `satset-core/src/test/java/com/satset/transaction/service/reconcile/TransactionReconcileServiceTest.java`

**Interfaces:**
- Consumes:
  - `TransactionRepository.findByStatusAndCreatedAtBefore(...)` (Task 1)
  - `TransactionRepository.findById(UUID) : Optional<Transactions>`
  - `DenomRepository.findDenomInfoById(UUID) : Optional<DenomInfo>` (record: `code()`, `basePrice()`, ...)
  - `ProviderPort.sendTransaction(String targetNumber, String denomCode, BigDecimal amount, String refId) : ProviderResponse`
  - `TransactionDomainService.reconcileProviderResult(Transactions tx, ProviderResponse resp, String walletId, DenomInfo denom) : void`
  - `Transactions` getters: `getId`, `getStatus`, `getRefNo`, `getTargetNumber`, `getTotal`, `getWalletId`, `getProductDenomId`, `getCreatedAt`
  - `@LogContext` annotation: `com.satset.shared.logging.LogContext`
- Produces: `TransactionReconcileService` Spring `@Service` with public `void reconcileStalePending()` and an 8-arg constructor `(TransactionRepository, DenomRepository, ProviderPort, TransactionDomainService, TransactionTemplate, long staleAfterMs, int batchSize, long maxAgeMs)`.

- [ ] **Step 1: Write the failing test**

Create `TransactionReconcileServiceTest.java`:

```java
package com.satset.transaction.service.reconcile;

import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.model.*;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.service.topup.TransactionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionReconcileServiceTest {

    @Mock TransactionRepository txRepo;
    @Mock DenomRepository denomRepo;
    @Mock ProviderPort provider;
    @Mock TransactionDomainService txService;
    @Mock TransactionTemplate transactionTemplate;

    TransactionReconcileService reconcile;

    UUID denomId = UUID.randomUUID();
    DenomInfo denom = new DenomInfo(denomId, "xld25", "XL 25K", "XL",
            new BigDecimal("25000.00"), BigDecimal.ZERO, new BigDecimal("24500.00"), true, false);

    long staleAfterMs = 120_000L;
    int batchSize = 100;
    long maxAgeMs = 21_600_000L; // 6h

    @BeforeEach
    void setUp() {
        // run the TransactionTemplate callback inline
        lenient().doAnswer(inv -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> c = inv.getArgument(0);
            c.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        reconcile = new TransactionReconcileService(
                txRepo, denomRepo, provider, txService, transactionTemplate,
                staleAfterMs, batchSize, maxAgeMs);
    }

    private Transactions stale() {
        Transactions tx = new Transactions();
        tx.setId(UUID.randomUUID());
        tx.setWalletId("w1");
        tx.setProductDenomId(denomId);
        tx.setTargetNumber("0878");
        tx.setTotal(new BigDecimal("25000.00"));
        tx.setStatus(TransactionStatus.PROCESSING);
        return tx; // createdAt null → age check skipped
    }

    private ProviderResponse ok() {
        return new ProviderResponse(ProviderStatus.SUCCESS, "REF", "SN", "Sukses", new BigDecimal("24500"));
    }

    @Test
    void repollsStale_andSettles() {
        Transactions tx = stale();
        when(txRepo.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(), any()))
                .thenReturn(List.of(tx));
        when(txRepo.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(denomRepo.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        ProviderResponse resp = ok();
        when(provider.sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx.getId().toString()))
                .thenReturn(resp);

        reconcile.reconcileStalePending();

        verify(provider).sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx.getId().toString());
        verify(txService).reconcileProviderResult(tx, resp, "w1", denom);
    }

    @Test
    void usesStoredRefNo_notUuid() {
        Transactions tx = stale();
        tx.setRefNo("2026072300042");
        when(txRepo.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(), any()))
                .thenReturn(List.of(tx));
        when(txRepo.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(denomRepo.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        when(provider.sendTransaction("0878", "xld25", new BigDecimal("25000.00"), "2026072300042"))
                .thenReturn(ok());

        reconcile.reconcileStalePending();

        verify(provider).sendTransaction("0878", "xld25", new BigDecimal("25000.00"), "2026072300042");
    }

    @Test
    void empty_doesNothing() {
        when(txRepo.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(), any()))
                .thenReturn(List.of());

        reconcile.reconcileStalePending();

        verifyNoInteractions(provider, txService);
    }

    @Test
    void oneRowThrows_doesNotPoisonOtherRows() {
        Transactions tx1 = stale();
        Transactions tx2 = stale();
        when(txRepo.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(), any()))
                .thenReturn(List.of(tx1, tx2));
        when(txRepo.findById(tx1.getId())).thenReturn(Optional.of(tx1));
        when(txRepo.findById(tx2.getId())).thenReturn(Optional.of(tx2));
        when(denomRepo.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        when(provider.sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx1.getId().toString()))
                .thenThrow(new RuntimeException("boom"));
        ProviderResponse resp2 = ok();
        when(provider.sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx2.getId().toString()))
                .thenReturn(resp2);

        reconcile.reconcileStalePending();

        verify(txService, never()).reconcileProviderResult(eq(tx1), any(), any(), any());
        verify(txService).reconcileProviderResult(tx2, resp2, "w1", denom);
    }

    @Test
    void reFetchGuard_skipsRowNoLongerProcessing() {
        Transactions tx = stale();
        Transactions settled = stale();
        settled.setId(tx.getId());
        settled.setStatus(TransactionStatus.SUCCESS); // webhook settled between scan and settle
        when(txRepo.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(), any()))
                .thenReturn(List.of(tx));
        when(txRepo.findById(tx.getId())).thenReturn(Optional.of(settled));

        reconcile.reconcileStalePending();

        verifyNoInteractions(provider, txService);
    }

    @Test
    void giveUpCutoff_pastMaxAge_noRepoll_staysProcessing() {
        Transactions tx = stale();
        tx.setCreatedAt(LocalDateTime.now().minusHours(7)); // > 6h maxAge
        when(txRepo.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(), any()))
                .thenReturn(List.of(tx));
        when(txRepo.findById(tx.getId())).thenReturn(Optional.of(tx));

        reconcile.reconcileStalePending();

        verifyNoInteractions(provider, txService);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PROCESSING);
    }

    @Test
    void scan_usesBatchSizeAndOldestFirst() {
        when(txRepo.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(), any()))
                .thenReturn(List.of());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        reconcile.reconcileStalePending();

        verify(txRepo).findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(batchSize);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").isAscending()).isTrue();
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q -pl satset-core test -Dtest=TransactionReconcileServiceTest`
Expected: FAIL — `TransactionReconcileService` does not exist (compile error).

- [ ] **Step 3: Write the service**

Create `TransactionReconcileService.java`:

```java
package com.satset.transaction.service.reconcile;

import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.logging.LogContext;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.TransactionStatus;
import com.satset.transaction.model.Transactions;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.service.topup.TransactionDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Core-side safety net for topups stuck in {@link TransactionStatus#PROCESSING} because
 * Digiflazz returned "Pending". Re-POSTs {@code /transaction} with the same ref_id
 * (idempotent, no re-charge — doubles as a status check) and settles via
 * {@link TransactionDomainService#reconcileProviderResult}. The primary settler is the
 * Fly webhook; this backstops late/missing webhooks (home IP is DF-whitelisted).
 *
 * <p>ponytail: batch cap per run so a backlog can't stampede DF's rate limit (rc 85).
 * Widen {@code topup.reconcile.batch-size} if throughput needs it.
 */
@Slf4j
@Service
@LogContext("Reconcile")
public class TransactionReconcileService {

    private final TransactionRepository txRepo;
    private final DenomRepository denomRepo;
    private final ProviderPort provider;
    private final TransactionDomainService txService;
    private final TransactionTemplate transactionTemplate;
    private final long staleAfterMs;
    private final int batchSize;
    private final long maxAgeMs;

    public TransactionReconcileService(
            TransactionRepository txRepo,
            DenomRepository denomRepo,
            ProviderPort provider,
            TransactionDomainService txService,
            TransactionTemplate transactionTemplate,
            @Value("${topup.reconcile.stale-after-ms:120000}") long staleAfterMs,
            @Value("${topup.reconcile.batch-size:100}") int batchSize,
            @Value("${topup.reconcile.max-age-ms:21600000}") long maxAgeMs) {
        this.txRepo = txRepo;
        this.denomRepo = denomRepo;
        this.provider = provider;
        this.txService = txService;
        this.transactionTemplate = transactionTemplate;
        this.staleAfterMs = staleAfterMs;
        this.batchSize = batchSize;
        this.maxAgeMs = maxAgeMs;
    }

    /**
     * NOT {@code @Transactional}: each row settles in its OWN transaction via
     * {@link #transactionTemplate}, so one failing row can't mark a shared transaction
     * rollback-only and poison every other settlement in the batch (a bare per-row
     * try/catch inside one {@code @Transactional} does NOT protect against this — Spring
     * still throws {@code UnexpectedRollbackException} at commit).
     */
    @Scheduled(fixedDelayString = "${topup.reconcile.interval-ms:60000}")
    public void reconcileStalePending() {
        LocalDateTime cutoff = LocalDateTime.now().minusNanos(staleAfterMs * 1_000_000);
        List<Transactions> stale = txRepo.findByStatusAndCreatedAtBefore(
                TransactionStatus.PROCESSING, cutoff,
                PageRequest.of(0, batchSize, Sort.by(Sort.Direction.ASC, "createdAt")));
        if (stale.isEmpty()) return;

        log.info("Reconcile: {} stale PROCESSING tx", stale.size());
        for (Transactions row : stale) {
            try {
                transactionTemplate.executeWithoutResult(status -> settleOne(row));
            } catch (Exception e) {
                log.error("Reconcile error tx {}: {}", row.getId(), e.getMessage(), e);
                // leave PROCESSING → retried next run; other rows in this batch unaffected
            }
        }
    }

    private void settleOne(Transactions row) {
        Transactions tx = txRepo.findById(row.getId()).orElse(null);
        if (tx == null || tx.getStatus() != TransactionStatus.PROCESSING) return; // webhook already settled

        if (tx.getCreatedAt() != null
                && Duration.between(tx.getCreatedAt(), LocalDateTime.now()).toMillis() > maxAgeMs) {
            log.error("ALERT: tx {} stuck PROCESSING > maxAge ({}h), needs manual Ops",
                    tx.getId(), maxAgeMs / 3_600_000);
            return; // give up re-polling; row stays PROCESSING (DF may still settle)
        }

        DenomInfo denom = denomRepo.findDenomInfoById(tx.getProductDenomId()).orElse(null);
        if (denom == null) {
            log.warn("Reconcile skip: denom {} gone for tx {}", tx.getProductDenomId(), tx.getId());
            return;
        }
        ProviderResponse resp = provider.sendTransaction(
                tx.getTargetNumber(), denom.code(), tx.getTotal(), refIdFor(tx));
        txService.reconcileProviderResult(tx, resp, tx.getWalletId(), denom);
    }

    /** Pre-ref_no rows have a null ref_no; fall back to the UUID so the re-POST still matches DF. */
    private static String refIdFor(Transactions tx) {
        return tx.getRefNo() != null ? tx.getRefNo() : tx.getId().toString();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -pl satset-core test -Dtest=TransactionReconcileServiceTest`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/java/com/satset/transaction/service/reconcile/TransactionReconcileService.java \
        satset-core/src/test/java/com/satset/transaction/service/reconcile/TransactionReconcileServiceTest.java
git commit -m "feat(transaction): reinstate reconcile sweep for stuck PROCESSING topups

Re-poll DF (idempotent) + settle via reconcileProviderResult; per-row tx
isolation; give-up ALERT past max-age. @LogContext(\"Reconcile\")."
```

---

### Task 3: Wire scheduling on + config defaults

**Files:**
- Modify: `satset-core/src/main/java/com/satset/SatsetGoApplication.java`
- Modify: `satset-core/src/main/resources/application.yml`

**Interfaces:**
- Consumes: `TransactionReconcileService.reconcileStalePending()` (`@Scheduled`, Task 2).

- [ ] **Step 1: Enable scheduling**

In `SatsetGoApplication.java`, add the import and annotation:

```java
import org.springframework.scheduling.annotation.EnableScheduling;
```

```java
@SpringBootApplication
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class SatsetGoApplication {
```

- [ ] **Step 2: Add config defaults**

In `application.yml`, add a top-level `topup` block (defaults also live in the `@Value` fallbacks; this documents + centralizes them):

```yaml
topup:
  reconcile:
    interval-ms: 60000       # @Scheduled fixedDelay between sweeps
    stale-after-ms: 120000   # a row must be older than this to be swept
    batch-size: 100          # max rows per run (rate-limit guard vs DF rc 85)
    max-age-ms: 21600000     # 6h — past this, stop re-polling and ALERT for manual Ops
```

- [ ] **Step 3: Verify the context loads with scheduling + bean wired**

Run: `mvn -q -pl satset-core -am test -Dtest=PurchaseFlowIntegrationTest`
Expected: PASS — full `@SpringBootTest` context loads, proving `@EnableScheduling`, the `TransactionReconcileService` bean, and the derived finder (Task 1) all wire without error.

- [ ] **Step 4: Commit**

```bash
git add satset-core/src/main/java/com/satset/SatsetGoApplication.java \
        satset-core/src/main/resources/application.yml
git commit -m "feat(transaction): enable scheduling + reconcile config defaults"
```

---

## Final verification

- [ ] Run the full core test suite: `mvn -q -pl satset-core -am test`
- [ ] Expected: BUILD SUCCESS, no regressions.
- [ ] Update `Tasks.md`: mark the Reconcile item `[x]`.
- [ ] Update Google Tasks list `Z184dEJwWFlUSG1GTkdIYQ`.

## Self-review notes

- **Spec coverage:** service (Task 2) · finder (Task 1) · `@EnableScheduling` + config (Task 3) · anti-double-settle = existing scan-filter + re-fetch guard (Task 2 test `reFetchGuard_skipsRowNoLongerProcessing`) + `@Version` (already on entity, not this plan's code) · give-up cutoff (Task 2 test + service) · `@LogContext("Reconcile")`.
- **Deliberate deviation from spec:** the spec's "Repo — `@DataJpaTest`" test is dropped (Task 1 note) — derived query, validated at startup, no `@DataJpaTest` precedent. Flag to reviewer.
- **Type consistency:** finder signature identical in Task 1, Task 2 mocks, and service. Constructor is 8-arg in service + test. `reconcileProviderResult(tx, resp, walletId, denom)` matches `TransactionDomainService`.
