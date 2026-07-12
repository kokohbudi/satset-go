# Accounting Slice 0+1 (Cost Snapshot + Margin/P&L) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Record cost + margin on every successful purchase, and expose an admin-only P&L report (revenue, COGS, margin) with a per-product breakdown.

**Architecture:** Slice 0 snapshots `costPrice` (from provider cost, falling back to `denom.basePrice`) and `margin = total − costPrice` onto the existing `Transactions` row at the SUCCESS branch. Slice 1 adds aggregate JPQL queries over that table, a thin `AccountingService`, and an admin Thymeleaf report page. No new DB schema, no new entity — so `CoreDataSourceConfig` is untouched.

**Tech Stack:** Spring Boot 4, Java 25, Hibernate/JPA (PostgreSQL), Thymeleaf, JUnit 5 + Mockito + AssertJ, Testcontainers.

## Global Constraints

- **Java 25 / Spring Boot 4.0.1**, Maven, entity == domain model (no mappers).
- **DDL auto = `update`** (dev) / `validate` (prod) — new columns are added by Hibernate in dev; prod needs the columns to exist before `validate`.
- **BigDecimal precision 15, scale 2** for money columns (10,2 for fees).
- **Margin is admin-only** — never add it to `TransactionDTO` (reseller-facing) or the reseller dashboard.
- **Permission pattern**: `@PreAuthorize("hasRole('" + OmniConstants.PERM_X + "')")`; perms are realm roles like `REALM_view_reports` (authority `ROLE_REALM_view_reports` after mapper).
- **TDD strict**: Red → Green → Refactor. Failing test first, always.
- **Error handling**: log server-side, never expose `e.getMessage()` to clients.
- **Git deletion/rename**: `git rm` (not applicable here — no deletions).

---

### Task 1: Snapshot cost + margin on successful purchase (Slice 0)

**Files:**
- Modify: `satset-core/src/main/java/com/satset/shared/model/DenomInfo.java`
- Modify: `satset-core/src/main/java/com/satset/catalog/repository/DenomRepository.java` (JPQL projection)
- Modify: `satset-core/src/main/java/com/satset/transaction/model/ProviderResponse.java`
- Modify: `satset-core/src/main/java/com/satset/transaction/client/MockProviderAdapter.java`
- Modify: `satset-core/src/main/java/com/satset/transaction/model/Transactions.java`
- Modify: `satset-core/src/main/java/com/satset/transaction/service/TransactionDomainService.java:99-107`
- Test: `satset-core/src/test/java/com/satset/transaction/service/TransactionDomainServiceTest.java`

**Interfaces:**
- Consumes: nothing (first task).
- Produces:
  - `DenomInfo(UUID id, String code, String name, String productName, BigDecimal price, BigDecimal adminFee, BigDecimal basePrice, boolean active, boolean deleted)` — new `basePrice` accessor.
  - `ProviderResponse(boolean success, String referenceNumber, String serialNumber, String message, BigDecimal cost)` — new nullable `cost`.
  - `Transactions.getCostPrice()` / `getMargin()` (BigDecimal, nullable).

- [ ] **Step 1: Write the failing tests**

Add three tests to `TransactionDomainServiceTest`. First, update the `setUp()` `denom` to carry a `basePrice` (insert `new BigDecimal("4600.00")` before `true, false`):

```java
denom = new DenomInfo(
    denomId, "TLKM5", "Telkomsel 5K", "Telkomsel",
    new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("4600.00"),
    true, false
);
```

Then add the new tests (import `org.mockito.ArgumentCaptor`):

```java
@Test
void createPurchase_Success_SnapshotsMargin_FallbackToBasePrice() throws InsufficientBalanceException {
    when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom)); // basePrice 4600, total 5000
    when(transactionRepository.save(any(Transactions.class))).thenAnswer(inv -> {
        Transactions tx = inv.getArgument(0);
        if (tx.getId() == null) tx.setId(UUID.randomUUID());
        return tx;
    });
    // provider reports no cost -> fallback to basePrice
    when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class)))
            .thenReturn(new ProviderResponse(true, "REF-1", "SN-1", "OK", null));

    transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

    ArgumentCaptor<Transactions> captor = ArgumentCaptor.forClass(Transactions.class);
    verify(transactionRepository, atLeastOnce()).save(captor.capture());
    Transactions saved = captor.getValue(); // last save = SUCCESS state
    assertEquals(new BigDecimal("4600.00"), saved.getCostPrice());
    assertEquals(new BigDecimal("400.00"), saved.getMargin());
}

@Test
void createPurchase_Success_ProviderCostOverridesBasePrice() throws InsufficientBalanceException {
    when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
    when(transactionRepository.save(any(Transactions.class))).thenAnswer(inv -> {
        Transactions tx = inv.getArgument(0);
        if (tx.getId() == null) tx.setId(UUID.randomUUID());
        return tx;
    });
    when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class)))
            .thenReturn(new ProviderResponse(true, "REF-2", "SN-2", "OK", new BigDecimal("4800.00")));

    transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

    ArgumentCaptor<Transactions> captor = ArgumentCaptor.forClass(Transactions.class);
    verify(transactionRepository, atLeastOnce()).save(captor.capture());
    Transactions saved = captor.getValue();
    assertEquals(new BigDecimal("4800.00"), saved.getCostPrice());
    assertEquals(new BigDecimal("200.00"), saved.getMargin());
}

@Test
void createPurchase_Failed_LeavesCostAndMarginNull() throws InsufficientBalanceException {
    when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
    when(transactionRepository.save(any(Transactions.class))).thenAnswer(inv -> {
        Transactions tx = inv.getArgument(0);
        if (tx.getId() == null) tx.setId(UUID.randomUUID());
        return tx;
    });
    when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class)))
            .thenReturn(new ProviderResponse(false, null, null, "Timeout", null));

    transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

    ArgumentCaptor<Transactions> captor = ArgumentCaptor.forClass(Transactions.class);
    verify(transactionRepository, atLeastOnce()).save(captor.capture());
    Transactions saved = captor.getValue();
    assertNull(saved.getCostPrice());
    assertNull(saved.getMargin());
}
```

Also update every other `new DenomInfo(...)` in this file (insert a basePrice arg before `active, deleted` — use `new BigDecimal("4600.00")` for the 5000 denom, `new BigDecimal("9000.00")` for the 10000 denoms, `new BigDecimal("4600.00")` for inactive) and every `new ProviderResponse(...)` (append `, null`).

- [ ] **Step 2: Run tests to verify they fail (compile error)**

Run: `cd satset-core && ./mvnw -q -Dtest=TransactionDomainServiceTest test`
Expected: FAIL — compile errors ("constructor DenomInfo cannot be applied", "ProviderResponse cannot be applied", "getCostPrice() not found").

- [ ] **Step 3: Add `basePrice` to `DenomInfo`**

Replace the record header:

```java
public record DenomInfo(
    UUID id,
    String code,
    String name,
    String productName,
    BigDecimal price,
    BigDecimal adminFee,
    BigDecimal basePrice,
    boolean active,
    boolean deleted
) {
```

(Leave `total()` and `isAvailable()` unchanged.)

- [ ] **Step 4: Update the `DenomInfo` projection query**

In `DenomRepository.findDenomInfoById`, add `d.basePrice` in the constructor position (after `d.adminFee`):

```java
@Query("""
        SELECT new com.satset.shared.model.DenomInfo(
        d.id, d.code, d.name, p.name, d.price, d.adminFee, d.basePrice, d.active, d.deleted
    )
    FROM ProductDenoms d
    LEFT JOIN Products p ON d.productId = p.id
    WHERE d.id = :id
    """)
Optional<DenomInfo> findDenomInfoById(UUID id);
```

- [ ] **Step 5: Add `cost` to `ProviderResponse`**

```java
public record ProviderResponse(
        boolean success,
        String referenceNumber,
        String serialNumber,
        String message,
        BigDecimal cost) {
}
```

Add `import java.math.BigDecimal;`.

- [ ] **Step 6: Update `MockProviderAdapter` construction sites**

Success return → `return new ProviderResponse(true, ref, sn, "Transaksi berhasil", null);`
Failure return → `return new ProviderResponse(false, null, null, "Transaksi gagal dari provider", null);`

(Mock reports no cost; the fallback-to-basePrice path is what runs.)

- [ ] **Step 7: Add `costPrice` + `margin` columns to `Transactions`**

After the `total` field:

```java
@Column(name = "cost_price", precision = 15, scale = 2)
private BigDecimal costPrice;

@Column(precision = 15, scale = 2)
private BigDecimal margin;
```

- [ ] **Step 8: Set cost + margin in the SUCCESS branch**

In `TransactionDomainService.createPurchase`, inside `if (response.success()) { ... }` after setting `serialNumber` and before `transaction = transactionRepository.save(transaction);`:

```java
BigDecimal costPrice = response.cost() != null ? response.cost() : denom.basePrice();
transaction.setCostPrice(costPrice);
transaction.setMargin(costPrice != null ? total.subtract(costPrice) : null);
```

(Guards a null `basePrice` — `total.subtract(null)` would NPE. FAILED/REFUNDED branch leaves both null.)

- [ ] **Step 9: Fix remaining compile errors across the module**

Run: `cd satset-core && ./mvnw -q -o test-compile`
Expected: compile errors at any other `new DenomInfo(...)` / `new ProviderResponse(...)` call sites (e.g. `PurchaseFlowIntegrationTest`, `TransactionControllerTest`, catalog service/tests). For each, insert the new arg: `basePrice` before `active, deleted` for `DenomInfo`; `, null` appended for `ProviderResponse`. Re-run until it compiles.

- [ ] **Step 10: (dev data) seed `basePrice` so dev reports show margin**

In `DataSeeder`, wherever `ProductDenoms` are built, set `basePrice` to a value below `price` (e.g. `denom.setBasePrice(denom.getPrice().multiply(new BigDecimal("0.92")))`). If denoms are seeded in a loop, one line covers all. Skip if seeding already sets it.
`// ponytail: rough 8% margin for demo data only`

- [ ] **Step 11: Run tests to verify they pass**

Run: `cd satset-core && ./mvnw -q -Dtest=TransactionDomainServiceTest test`
Expected: PASS (all tests, including the three new ones).

- [ ] **Step 12: Run the full module test suite (no regressions)**

Run: `cd satset-core && ./mvnw -q test`
Expected: PASS.

- [ ] **Step 13: Commit**

```bash
git add satset-core/src/main/java/com/satset satset-core/src/test/java/com/satset/transaction/service/TransactionDomainServiceTest.java
git commit -m "feat(accounting): snapshot cost + margin on successful purchase"
```

---

### Task 2: Aggregate P&L queries + report DTOs (Slice 1a)

**Files:**
- Create: `satset-core/src/main/java/com/satset/accounting/dto/PnlSummary.java`
- Create: `satset-core/src/main/java/com/satset/accounting/dto/PnlRow.java`
- Modify: `satset-core/src/main/java/com/satset/transaction/repository/TransactionRepository.java`
- Test: `satset-core/src/test/java/com/satset/accounting/AccountingRepositoryTest.java`

**Interfaces:**
- Consumes: `Transactions` (with `total`, `costPrice`, `margin`, `status`, `createdAt`, `productName` from Task 1).
- Produces:
  - `PnlSummary(BigDecimal revenue, BigDecimal cogs, BigDecimal margin, long count)`
  - `PnlRow(String label, BigDecimal revenue, BigDecimal cogs, BigDecimal margin, long count)`
  - `TransactionRepository.summarizePnl(LocalDateTime from, LocalDateTime to)` → `PnlSummary`
  - `TransactionRepository.summarizePnlByProduct(LocalDateTime from, LocalDateTime to)` → `List<PnlRow>`

- [ ] **Step 1: Write the failing repository test**

Mirror the class-level container/annotation setup of `satset-core/src/test/java/com/satset/transaction/web/PurchaseFlowIntegrationTest.java` (same `@SpringBootTest` + Testcontainers support). Body:

```java
@Test
void summarizePnl_sumsOnlySuccessRowsInRange() {
    LocalDateTime now = LocalDateTime.now();
    // in-range SUCCESS: revenue 6500, cost 6000, margin 500
    save(6500, 6000, 500, TransactionStatus.SUCCESS, now.minusHours(1), "IM3");
    // in-range SUCCESS second product
    save(11000, 10000, 1000, TransactionStatus.SUCCESS, now.minusHours(2), "Telkomsel");
    // FAILED -> excluded
    save(5000, null, null, TransactionStatus.FAILED, now.minusHours(1), "IM3");
    // out of range -> excluded
    save(9999, 9000, 999, TransactionStatus.SUCCESS, now.minusDays(5), "IM3");

    LocalDateTime from = now.minusDays(1);
    LocalDateTime to = now.plusMinutes(1);

    PnlSummary s = transactionRepository.summarizePnl(from, to);
    assertThat(s.revenue()).isEqualByComparingTo("17500");
    assertThat(s.cogs()).isEqualByComparingTo("16000");
    assertThat(s.margin()).isEqualByComparingTo("1500");
    assertThat(s.count()).isEqualTo(2);

    List<PnlRow> rows = transactionRepository.summarizePnlByProduct(from, to);
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).label()).isEqualTo("Telkomsel"); // ordered by margin desc
    assertThat(rows.get(0).margin()).isEqualByComparingTo("1000");
}

private void save(long total, Long cost, Long margin, TransactionStatus status,
                  LocalDateTime createdAt, String product) {
    Transactions t = new Transactions();
    t.setStoreId(UUID.randomUUID());
    t.setProductDenomId(UUID.randomUUID());
    t.setProductName(product);
    t.setDenomName(product + " denom");
    t.setTargetNumber("081200000000");
    t.setPrice(new BigDecimal(total));
    t.setAdminFee(BigDecimal.ZERO);
    t.setTotal(new BigDecimal(total));
    t.setCostPrice(cost != null ? new BigDecimal(cost) : null);
    t.setMargin(margin != null ? new BigDecimal(margin) : null);
    t.setStatus(status);
    Transactions saved = transactionRepository.save(t);
    // createdAt is @CreatedDate (auto). Override to control range:
    saved.setCreatedAt(createdAt);
    transactionRepository.saveAndFlush(saved);
}
```

Autowire `TransactionRepository transactionRepository`. Imports: `PnlSummary`, `PnlRow`, `Transactions`, `TransactionStatus`, `BigDecimal`, `LocalDateTime`, `List`, `UUID`, AssertJ `assertThat`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd satset-core && ./mvnw -q -Dtest=AccountingRepositoryTest test`
Expected: FAIL — `PnlSummary`/`PnlRow` do not exist, `summarizePnl` not defined.

- [ ] **Step 3: Create the DTO records**

`PnlSummary.java`:

```java
package com.satset.accounting.dto;

import java.math.BigDecimal;

public record PnlSummary(BigDecimal revenue, BigDecimal cogs, BigDecimal margin, long count) {
}
```

`PnlRow.java`:

```java
package com.satset.accounting.dto;

import java.math.BigDecimal;

public record PnlRow(String label, BigDecimal revenue, BigDecimal cogs, BigDecimal margin, long count) {
}
```

- [ ] **Step 4: Add the aggregate queries to `TransactionRepository`**

Add imports (`com.satset.accounting.dto.PnlSummary`, `com.satset.accounting.dto.PnlRow`, `java.util.List`, `org.springframework.data.repository.query.Param`) and methods:

```java
@Query("""
        SELECT new com.satset.accounting.dto.PnlSummary(
            COALESCE(SUM(t.total), 0), COALESCE(SUM(t.costPrice), 0),
            COALESCE(SUM(t.margin), 0), COUNT(t))
        FROM Transactions t
        WHERE t.status = com.satset.transaction.model.TransactionStatus.SUCCESS
          AND t.createdAt >= :from AND t.createdAt < :to
        """)
PnlSummary summarizePnl(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

@Query("""
        SELECT new com.satset.accounting.dto.PnlRow(
            t.productName, COALESCE(SUM(t.total), 0), COALESCE(SUM(t.costPrice), 0),
            COALESCE(SUM(t.margin), 0), COUNT(t))
        FROM Transactions t
        WHERE t.status = com.satset.transaction.model.TransactionStatus.SUCCESS
          AND t.createdAt >= :from AND t.createdAt < :to
        GROUP BY t.productName
        ORDER BY SUM(t.margin) DESC
        """)
List<PnlRow> summarizePnlByProduct(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
```

`// ponytail: raw aggregate over transactions; add a daily rollup table only if this query gets slow at volume`

Note: rows where `margin` is null (unknown cost) still count in `revenue`/`count` but not `margin` — SUM ignores nulls. Acceptable per spec (data-quality caveat).

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd satset-core && ./mvnw -q -Dtest=AccountingRepositoryTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add satset-core/src/main/java/com/satset/accounting satset-core/src/main/java/com/satset/transaction/repository/TransactionRepository.java satset-core/src/test/java/com/satset/accounting/AccountingRepositoryTest.java
git commit -m "feat(accounting): P&L aggregate queries + summary DTOs"
```

---

### Task 3: AccountingService (Slice 1b)

**Files:**
- Create: `satset-core/src/main/java/com/satset/accounting/dto/PnlReport.java`
- Create: `satset-core/src/main/java/com/satset/accounting/service/AccountingService.java`
- Test: `satset-core/src/test/java/com/satset/accounting/service/AccountingServiceTest.java`

**Interfaces:**
- Consumes: `TransactionRepository.summarizePnl`, `summarizePnlByProduct`, `PnlSummary`, `PnlRow` (Task 2).
- Produces:
  - `PnlReport(LocalDate from, LocalDate to, PnlSummary summary, List<PnlRow> byProduct)`
  - `AccountingService.report(LocalDate from, LocalDate to)` → `PnlReport`. Converts dates to a half-open `[from 00:00, to+1 00:00)` range.

- [ ] **Step 1: Write the failing test**

```java
package com.satset.accounting.service;

import com.satset.accounting.dto.PnlReport;
import com.satset.accounting.dto.PnlRow;
import com.satset.accounting.dto.PnlSummary;
import com.satset.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountingServiceTest {

    @Mock TransactionRepository transactionRepository;
    @InjectMocks AccountingService accountingService;

    @Test
    void report_usesHalfOpenRange_andPassesThroughAggregates() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 10);
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 11, 0, 0); // to + 1 day, exclusive

        PnlSummary summary = new PnlSummary(new BigDecimal("17500"), new BigDecimal("16000"),
                new BigDecimal("1500"), 2);
        List<PnlRow> rows = List.of(new PnlRow("Telkomsel", new BigDecimal("11000"),
                new BigDecimal("10000"), new BigDecimal("1000"), 1));
        when(transactionRepository.summarizePnl(start, end)).thenReturn(summary);
        when(transactionRepository.summarizePnlByProduct(start, end)).thenReturn(rows);

        PnlReport report = accountingService.report(from, to);

        assertThat(report.from()).isEqualTo(from);
        assertThat(report.to()).isEqualTo(to);
        assertThat(report.summary()).isEqualTo(summary);
        assertThat(report.byProduct()).isEqualTo(rows);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd satset-core && ./mvnw -q -Dtest=AccountingServiceTest test`
Expected: FAIL — `AccountingService` / `PnlReport` do not exist.

- [ ] **Step 3: Create `PnlReport`**

```java
package com.satset.accounting.dto;

import java.time.LocalDate;
import java.util.List;

public record PnlReport(LocalDate from, LocalDate to, PnlSummary summary, List<PnlRow> byProduct) {
}
```

- [ ] **Step 4: Create `AccountingService`**

```java
package com.satset.accounting.service;

import com.satset.accounting.dto.PnlReport;
import com.satset.accounting.dto.PnlRow;
import com.satset.accounting.dto.PnlSummary;
import com.satset.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountingService {

    private final TransactionRepository transactionRepository;

    public AccountingService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public PnlReport report(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay(); // half-open: includes all of `to`
        PnlSummary summary = transactionRepository.summarizePnl(start, end);
        List<PnlRow> byProduct = transactionRepository.summarizePnlByProduct(start, end);
        return new PnlReport(from, to, summary, byProduct);
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd satset-core && ./mvnw -q -Dtest=AccountingServiceTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add satset-core/src/main/java/com/satset/accounting satset-core/src/test/java/com/satset/accounting/service/AccountingServiceTest.java
git commit -m "feat(accounting): AccountingService P&L report"
```

---

### Task 4: Admin P&L report page + permission (Slice 1c)

**Files:**
- Modify: `satset-core/src/main/java/com/satset/shared/constant/OmniConstants.java`
- Create: `satset-core/src/main/java/com/satset/accounting/web/AccountingReportController.java`
- Create: `satset-core/src/main/resources/templates/pages/admin/pnl-report.html`
- Test: `satset-core/src/test/java/com/satset/accounting/web/AccountingReportControllerSecurityTest.java`
- Ops: Keycloak realm role `view_reports` + testcontainers realm JSON

**Interfaces:**
- Consumes: `AccountingService.report`, `PnlReport` (Task 3); `OmniConstants.PERM_VIEW_REPORTS`.
- Produces: `GET /admin/reports/pnl?from=&to=` (admin-only) rendering `pages/admin/pnl-report`.

- [ ] **Step 1: Write the failing security test**

Mirror the class-level annotations of `satset-core/src/test/java/com/satset/transaction/web/TransactionControllerSecurityTest.java` (same MockMvc/security bootstrap). Two cases:

```java
@Test
void pnlReport_forbidden_withoutReportsRole() throws Exception {
    mockMvc.perform(get("/admin/reports/pnl")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REALM_view_catalog"))))
        .andExpect(status().isForbidden());
}

@Test
void pnlReport_ok_withReportsRole() throws Exception {
    mockMvc.perform(get("/admin/reports/pnl")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REALM_view_reports"))))
        .andExpect(status().isOk());
}
```

Match the existing test's imports/helpers for `jwt()` and `mockMvc` (copy from `TransactionControllerSecurityTest`). If that test stubs collaborators with `@MockBean`, add `@MockBean AccountingService accountingService;` and stub `report(...)` to return an empty `PnlReport` so the OK case renders.

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd satset-core && ./mvnw -q -Dtest=AccountingReportControllerSecurityTest test`
Expected: FAIL — no mapping for `/admin/reports/pnl` (404), `PERM_VIEW_REPORTS` undefined.

- [ ] **Step 3: Add the permission constant**

In `OmniConstants`, next to the other perms:

```java
public static final String PERM_VIEW_REPORTS = "REALM_view_reports";
```

- [ ] **Step 4: Create the controller**

```java
package com.satset.accounting.web;

import com.satset.accounting.dto.PnlReport;
import com.satset.accounting.service.AccountingService;
import com.satset.shared.constant.SatsetConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@Slf4j
public class AccountingReportController {

    private final AccountingService accountingService;

    public AccountingReportController(AccountingService accountingService) {
        this.accountingService = accountingService;
    }

    @GetMapping("/admin/reports/pnl")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_REPORTS + "')")
    public String pnlReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        LocalDate toDate = (to != null) ? to : LocalDate.now();
        LocalDate fromDate = (from != null) ? from : toDate.withDayOfMonth(1);
        if (fromDate.isAfter(toDate)) {
            fromDate = toDate.withDayOfMonth(1);
        }

        PnlReport report = accountingService.report(fromDate, toDate);
        model.addAttribute("currentPage", "reports");
        model.addAttribute("breadcrumb", "Laba Rugi");
        model.addAttribute("report", report);
        model.addAttribute("from", fromDate);
        model.addAttribute("to", toDate);
        return "pages/admin/pnl-report";
    }
}
```

- [ ] **Step 5: Create the Thymeleaf page**

Mirror the layout wiring of `satset-core/src/main/resources/templates/pages/admin/wallet-adjust.html` (same base-layout fragment, `currentPage`/`breadcrumb`). Content: a date-range GET filter, three summary tiles, and the per-product table. SSR-seeded from `report` (no client fetch needed — the filter is a plain GET reload).

```html
<div class="space-y-6">
  <form method="get" action="/admin/reports/pnl" class="flex flex-wrap items-end gap-3">
    <label class="text-sm">Dari
      <input type="date" name="from" th:value="${from}" class="block border rounded px-2 py-1"/>
    </label>
    <label class="text-sm">Sampai
      <input type="date" name="to" th:value="${to}" class="block border rounded px-2 py-1"/>
    </label>
    <button type="submit" class="bg-orange-600 text-white rounded px-4 py-2">Terapkan</button>
  </form>

  <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
    <div class="rounded-lg border p-4">
      <div class="text-sm text-gray-500">Pendapatan</div>
      <div class="text-2xl font-semibold" th:text="${'Rp ' + #numbers.formatInteger(report.summary().revenue(), 0, 'POINT')}">Rp 0</div>
    </div>
    <div class="rounded-lg border p-4">
      <div class="text-sm text-gray-500">Modal (COGS)</div>
      <div class="text-2xl font-semibold" th:text="${'Rp ' + #numbers.formatInteger(report.summary().cogs(), 0, 'POINT')}">Rp 0</div>
    </div>
    <div class="rounded-lg border p-4">
      <div class="text-sm text-gray-500">Margin</div>
      <div class="text-2xl font-semibold text-orange-600" th:text="${'Rp ' + #numbers.formatInteger(report.summary().margin(), 0, 'POINT')}">Rp 0</div>
      <div class="text-xs text-gray-400" th:text="${report.summary().count() + ' transaksi'}">0 transaksi</div>
    </div>
  </div>

  <table class="w-full text-sm border-collapse">
    <thead>
      <tr class="text-left border-b">
        <th class="py-2">Produk</th>
        <th class="py-2 text-right">Pendapatan</th>
        <th class="py-2 text-right">Modal</th>
        <th class="py-2 text-right">Margin</th>
        <th class="py-2 text-right">Transaksi</th>
      </tr>
    </thead>
    <tbody>
      <tr th:each="row : ${report.byProduct()}" class="border-b">
        <td class="py-2" th:text="${row.label()}">Produk</td>
        <td class="py-2 text-right" th:text="${#numbers.formatInteger(row.revenue(), 0, 'POINT')}">0</td>
        <td class="py-2 text-right" th:text="${#numbers.formatInteger(row.cogs(), 0, 'POINT')}">0</td>
        <td class="py-2 text-right text-orange-600" th:text="${#numbers.formatInteger(row.margin(), 0, 'POINT')}">0</td>
        <td class="py-2 text-right" th:text="${row.count()}">0</td>
      </tr>
      <tr th:if="${report.byProduct().isEmpty()}">
        <td colspan="5" class="py-6 text-center text-gray-400">Belum ada transaksi sukses pada rentang ini.</td>
      </tr>
    </tbody>
  </table>
</div>
```

Wrap this in the same base-layout `<html>`/fragment shell used by `wallet-adjust.html` (copy that file's outer structure, replace the content block).

- [ ] **Step 6: Run the security test to verify it passes**

Run: `cd satset-core && ./mvnw -q -Dtest=AccountingReportControllerSecurityTest test`
Expected: PASS (403 without role, 200 with `ROLE_REALM_view_reports`).

- [ ] **Step 7: Register the Keycloak realm role for runtime + integration tests**

Create realm role `view_reports` in the live realm (`satset-go`) via the Keycloak MCP (`create-role`) or admin console, and assign it to admin users. Add the same role to the testcontainers realm import `satset-core/src/test/resources/satset-go-realm-full.json` so any KC-booting integration test can grant it. (The security test above uses a mock JWT and does not need this.)

- [ ] **Step 8: Add a sidebar/nav entry (optional, if sidebar is static)**

If the admin sidebar is not fully role-attribute driven, add a "Laba Rugi" link to `/admin/reports/pnl` in the sidebar template, gated on the `view_reports` role. Skip if the sidebar is entirely Keycloak-attribute driven (add the `sidebar=1`/`url`/`display_name` attributes to the `view_reports` role instead).

- [ ] **Step 9: Run the full suite (no regressions)**

Run: `cd satset-core && ./mvnw -q test`
Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add satset-core/src/main/java/com/satset/accounting satset-core/src/main/java/com/satset/shared/constant/OmniConstants.java satset-core/src/main/resources/templates/pages/admin/pnl-report.html satset-core/src/test/java/com/satset/accounting/web/AccountingReportControllerSecurityTest.java satset-core/src/test/resources/satset-go-realm-full.json
git commit -m "feat(accounting): admin P&L report page + view_reports permission"
```

---

## Self-Review

**Spec coverage:**
- Slice 0 cost snapshot (basePrice fallback + provider override, null on failure) → Task 1 ✓
- `DenomInfo.basePrice`, `ProviderResponse.cost`, `Transactions.costPrice/margin` → Task 1 ✓
- Aggregate revenue/COGS/margin/count by product, SUCCESS-only, date range → Task 2 ✓
- `AccountingService` → Task 3 ✓
- Admin report page, SSR, date filter, ADMIN-only → Task 4 ✓
- Dashboard tiles → **relocated to the admin report page** (Task 4 tiles); NOT on the reseller dashboard, because margin is platform profit and must not leak to resellers. Deviation from spec noted here.
- Security: spec said `hasRole('ADMIN')`; codebase uses fine-grained realm perms, so this uses a new `PERM_VIEW_REPORTS` (`REALM_view_reports`) — closer to the real auth model. Deviation noted.
- Out of scope (double-entry, rollup, cache, provider balance) → untouched ✓

**Placeholder scan:** No TBD/TODO. Two "mirror the existing X" references (Testcontainers setup, base layout, security-test bootstrap) point at named concrete files to copy — necessary because those harness details are established patterns, not inventable inline.

**Type consistency:** `PnlSummary(revenue, cogs, margin, count)`, `PnlRow(label, revenue, cogs, margin, count)`, `PnlReport(from, to, summary, byProduct)`, `AccountingService.report(LocalDate, LocalDate)`, `summarizePnl(LocalDateTime, LocalDateTime)`, `summarizePnlByProduct(...)` — consistent across Tasks 2→3→4. `DenomInfo` new arg order (`...adminFee, basePrice, active, deleted`) consistent between Task 1 record, projection, and test constructions.
