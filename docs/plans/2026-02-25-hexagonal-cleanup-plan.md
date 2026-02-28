# Hexagonal Architecture Cleanup — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make all domain services inject port interfaces instead of JPA repository classes, enforcing the Hexagonal Architecture dependency rule across all 4 bounded contexts.

**Architecture:** Port-First approach — domain services only import from `domain.port.out` (never from `adapter.out`). JPA repositories directly `implements` port interfaces. No new adapter classes needed.

**Tech Stack:** Spring Boot 4.0.1, Spring Data JPA, Java 25, Mockito for tests

---

## Overview: What Changes Where

```
domain/service  →  domain/port/out   ✅ (target state)
domain/service  →  adapter/out       ❌ (current broken state, must fix)
```

Files that need port interface updates (extend existing port or add method):
- `catalog/domain/port/out/DenomRepositoryPort.java` — add `findById(UUID)`
- `catalog/domain/port/out/ProductRepositoryPort.java` — already complete ✅
- `catalog/domain/port/out/CategoryRepositoryPort.java` — already complete ✅
- `catalog/domain/port/out/DenomMetaRepositoryPort.java` — already complete ✅
- `identity/domain/port/out/UserRepositoryPort.java` — add `findById(UUID)`, `save(Users)`, `findByEmail(String)`
- `onboarding/domain/port/out/StoreRepositoryPort.java` — add `findById(UUID)`, `save(Stores)`, `findByEmail(String)`
- `onboarding/domain/port/out/OnboardingUserPort.java` — add `findByEmail(String)`, `save(Users)`
- `transaction/domain/port/out/TransactionRepositoryPort.java` — add `save(Transactions)`, `findById(UUID)`, `existsByStoreIdAndProductDenomId...()`
- `transaction/domain/port/out/StoreMutationRepositoryPort.java` — add `save(StoreMutations)`
- `transaction/domain/port/out/StoreBalancePort.java` — add `findById(UUID)`, `save(Stores)`

Domain services to fix (change constructor injection type):
- `catalog`: `CategoryDomainService`, `ProductDomainService`, `DenomDomainService`
- `identity`: `UserDomainService`, `UserManagementHelper`, `IdentityDomainService`
- `onboarding`: `StoreDomainService`, `AdminOnboardingDomainService`, `StoreOnboardingDomainService`, `RegistrationHelper`
- `transaction`: `TransactionDomainService`, `BalanceDomainService`

---

## Task 1: Fix catalog ports and domain services

**Files:**
- Modify: `src/main/java/com/omnip/catalog/domain/port/out/DenomRepositoryPort.java`
- Modify: `src/main/java/com/omnip/catalog/domain/service/CategoryDomainService.java`
- Modify: `src/main/java/com/omnip/catalog/domain/service/ProductDomainService.java`
- Modify: `src/main/java/com/omnip/catalog/domain/service/DenomDomainService.java`

**Step 1: Extend DenomRepositoryPort to add findById**

`DenomRepositoryPort` currently lacks `findById(UUID)` which `TransactionDomainService` needs (in Task 4). Add it now.

Edit `src/main/java/com/omnip/catalog/domain/port/out/DenomRepositoryPort.java`:

```java
package com.omnip.catalog.domain.port.out;

import com.omnip.catalog.domain.model.ProductDenoms;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Note: findById() is inherited from JpaRepository/CrudRepository.
 */
public interface DenomRepositoryPort {

    Optional<ProductDenoms> findById(UUID id);

    List<ProductDenoms> findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID productId);

    Optional<ProductDenoms> findByCode(String code);
}
```

**Step 2: Fix CategoryDomainService — inject port instead of JPA repo**

Edit `src/main/java/com/omnip/catalog/domain/service/CategoryDomainService.java`:

Replace:
```java
import com.omnip.catalog.adapter.out.persistence.CategoryJpaRepository;
```
With:
```java
import com.omnip.catalog.domain.port.out.CategoryRepositoryPort;
```

Replace field/constructor:
```java
private final CategoryJpaRepository categoryRepository;

public CategoryDomainService(CategoryJpaRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
}
```
With:
```java
private final CategoryRepositoryPort categoryRepository;

public CategoryDomainService(CategoryRepositoryPort categoryRepository) {
    this.categoryRepository = categoryRepository;
}
```

**Step 3: Fix ProductDomainService — inject ports instead of JPA repos**

Edit `src/main/java/com/omnip/catalog/domain/service/ProductDomainService.java`:

Replace imports:
```java
import com.omnip.catalog.adapter.out.persistence.CategoryJpaRepository;
import com.omnip.catalog.adapter.out.persistence.ProductJpaRepository;
```
With:
```java
import com.omnip.catalog.domain.port.out.CategoryRepositoryPort;
import com.omnip.catalog.domain.port.out.ProductRepositoryPort;
```

Replace fields/constructor:
```java
private final ProductJpaRepository productRepository;
private final CategoryJpaRepository categoryRepository;

public ProductDomainService(ProductJpaRepository productRepository, CategoryJpaRepository categoryRepository) {
```
With:
```java
private final ProductRepositoryPort productRepository;
private final CategoryRepositoryPort categoryRepository;

public ProductDomainService(ProductRepositoryPort productRepository, CategoryRepositoryPort categoryRepository) {
```

**Step 4: Fix DenomDomainService — inject ports instead of JPA repos**

Edit `src/main/java/com/omnip/catalog/domain/service/DenomDomainService.java`:

Replace imports:
```java
import com.omnip.catalog.adapter.out.persistence.DenomMetaJpaRepository;
import com.omnip.catalog.adapter.out.persistence.DenomJpaRepository;
import com.omnip.catalog.adapter.out.persistence.ProductJpaRepository;
```
With:
```java
import com.omnip.catalog.domain.port.out.DenomMetaRepositoryPort;
import com.omnip.catalog.domain.port.out.DenomRepositoryPort;
import com.omnip.catalog.domain.port.out.ProductRepositoryPort;
```

Replace fields/constructor:
```java
private final DenomJpaRepository denomRepository;
private final DenomMetaJpaRepository metaRepository;
private final ProductJpaRepository productRepository;

public DenomDomainService(DenomJpaRepository denomRepository,
        DenomMetaJpaRepository metaRepository,
        ProductJpaRepository productRepository) {
```
With:
```java
private final DenomRepositoryPort denomRepository;
private final DenomMetaRepositoryPort metaRepository;
private final ProductRepositoryPort productRepository;

public DenomDomainService(DenomRepositoryPort denomRepository,
        DenomMetaRepositoryPort metaRepository,
        ProductRepositoryPort productRepository) {
```

**Step 5: Verify build passes**

```bash
cd /Users/kokohbudi/myProjects/omnip-services-3
mvn compile -pl . -q 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`

**Step 6: Commit**

```bash
git add src/main/java/com/omnip/catalog/
git commit -m "refactor(catalog): inject port interfaces instead of JPA repos in domain services"
```

---

## Task 2: Fix identity ports and domain services

**Files:**
- Modify: `src/main/java/com/omnip/identity/domain/port/out/UserRepositoryPort.java`
- Modify: `src/main/java/com/omnip/identity/domain/port/out/KeycloakIdentityPort.java`
- Modify: `src/main/java/com/omnip/identity/domain/service/UserDomainService.java`
- Modify: `src/main/java/com/omnip/identity/domain/service/UserManagementHelper.java`
- Modify: `src/main/java/com/omnip/identity/domain/service/IdentityDomainService.java`

**Context:**
- `UserJpaRepository` already implements both `UserRepositoryPort` and `OnboardingUserPort`
- `KeycloakAdminClientService` is in `adapter/out/keycloak` — domain services should inject `KeycloakIdentityPort` instead
- `KeycloakIdentityPort` already exists at `identity/domain/port/out/KeycloakIdentityPort.java`

**Step 1: Extend UserRepositoryPort**

`UserDomainService` calls `.findByEmail()`, `.findByProviderUserId()`, `.save()`, and `.findByEmailInAndStoreId()` on `UserJpaRepository`. Add the missing methods to the port.

Edit `src/main/java/com/omnip/identity/domain/port/out/UserRepositoryPort.java`:

```java
package com.omnip.identity.domain.port.out;

import com.omnip.identity.domain.model.Users;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for user persistence in identity context.
 * Note: save() is inherited from JpaRepository/CrudRepository.
 */
public interface UserRepositoryPort {

    Optional<Users> findById(UUID id);

    Users save(Users user);

    Users findByEmail(String email);

    Users findByProviderUserId(String providerUserId);

    List<Users> findByEmailInAndStoreId(List<String> emails, String storeId);
}
```

**Step 2: Check what KeycloakIdentityPort exposes**

Read `src/main/java/com/omnip/identity/domain/port/out/KeycloakIdentityPort.java` first to understand what is already there. Then note which methods `UserDomainService` calls on `KeycloakAdminClientService`:
- `changeUserPassword(providerUserId, password)` — likely in port already
- `updateUserStatus(providerUserId, status)` — check if in port
- `createBackofficeUser(...)` — check if in port

If methods are missing from `KeycloakIdentityPort`, add them. Then update `UserDomainService` and `IdentityDomainService` to inject `KeycloakIdentityPort` instead of `KeycloakAdminClientService`.

**Step 3: Fix UserManagementHelper**

`UserManagementHelper` takes `UserJpaRepository` directly (both as field and as method parameter in `setUserStatus`).

Edit `src/main/java/com/omnip/identity/domain/service/UserManagementHelper.java`:

Replace import:
```java
import com.omnip.identity.adapter.out.persistence.UserJpaRepository;
```
With:
```java
import com.omnip.identity.domain.port.out.UserRepositoryPort;
```

Replace all occurrences of `UserJpaRepository` with `UserRepositoryPort` in:
- field declaration
- constructor parameter
- `setUserStatus` method signature
- `getRequestedUserOnStore` private method signature

**Step 4: Fix UserDomainService**

Edit `src/main/java/com/omnip/identity/domain/service/UserDomainService.java`:

Replace imports:
```java
import com.omnip.identity.adapter.out.persistence.UserJpaRepository;
import com.omnip.identity.adapter.out.keycloak.KeycloakAdminClientService;
```
With:
```java
import com.omnip.identity.domain.port.out.UserRepositoryPort;
import com.omnip.identity.domain.port.out.KeycloakIdentityPort;
```

Replace field/constructor types:
- `UserJpaRepository usersRepository` → `UserRepositoryPort usersRepository`
- `KeycloakAdminClientService keycloakAdminClientService` → `KeycloakIdentityPort keycloakAdminClientService`

**Step 5: Fix IdentityDomainService**

`IdentityDomainService` injects `KeycloakAdminClientService` directly.

Edit `src/main/java/com/omnip/identity/domain/service/IdentityDomainService.java`:

Replace import:
```java
import com.omnip.identity.adapter.out.keycloak.KeycloakAdminClientService;
```
With:
```java
import com.omnip.identity.domain.port.out.KeycloakIdentityPort;
```

Replace field/constructor:
- `KeycloakAdminClientService keycloakAdminClientService` → `KeycloakIdentityPort keycloakAdminClientService`

Note: `IdentityDomainService` also imports `KeycloakGroupDTO` and `KeycloakRoleDTO` from `adapter/in/web/dto`. These are DTOs, not adapters — keep them as-is since they're shared data structures. Only fix the `adapter/out` imports.

**Step 6: Verify build passes**

```bash
mvn compile -pl . -q 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`

**Step 7: Commit**

```bash
git add src/main/java/com/omnip/identity/
git commit -m "refactor(identity): inject port interfaces instead of JPA repos and Keycloak adapter in domain services"
```

---

## Task 3: Fix onboarding ports and domain services

**Files:**
- Modify: `src/main/java/com/omnip/onboarding/domain/port/out/StoreRepositoryPort.java`
- Modify: `src/main/java/com/omnip/onboarding/domain/port/out/OnboardingUserPort.java`
- Modify: `src/main/java/com/omnip/onboarding/domain/service/StoreDomainService.java`
- Modify: `src/main/java/com/omnip/onboarding/domain/service/AdminOnboardingDomainService.java`
- Modify: `src/main/java/com/omnip/onboarding/domain/service/StoreOnboardingDomainService.java`
- Modify: `src/main/java/com/omnip/onboarding/domain/service/RegistrationHelper.java`

**Context:**
- `StoreJpaRepository` already implements `StoreRepositoryPort` and `StoreBalancePort`
- `UserJpaRepository` already implements `OnboardingUserPort`
- Cross-context violation: onboarding domain services import `identity.adapter.out.persistence.UserJpaRepository` — fix by using `OnboardingUserPort`
- Same for `identity.adapter.out.keycloak.KeycloakAdminClientService` — fix by using `KeycloakOrganizationPort`

**Step 1: Extend StoreRepositoryPort**

`StoreDomainService`, `AdminOnboardingDomainService`, `StoreOnboardingDomainService` call `.findById()`, `.save()`, and `.findByEmail()` on `StoreJpaRepository`.

Edit `src/main/java/com/omnip/onboarding/domain/port/out/StoreRepositoryPort.java`:

```java
package com.omnip.onboarding.domain.port.out;

import com.omnip.onboarding.domain.model.Stores;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for store persistence in onboarding context.
 * Note: save() and findById() are inherited from JpaRepository/CrudRepository.
 */
public interface StoreRepositoryPort {

    boolean existsByReferralId(String referralId);

    Optional<Stores> findById(UUID id);

    Stores save(Stores store);

    Stores findByEmail(String email);
}
```

**Step 2: Extend OnboardingUserPort**

`AdminOnboardingDomainService` and `StoreOnboardingDomainService` call `.findByProviderUserId()` and `.save()` on `UserJpaRepository`. `RegistrationHelper` also calls `.findByEmail()`.

Edit `src/main/java/com/omnip/onboarding/domain/port/out/OnboardingUserPort.java`:

```java
package com.omnip.onboarding.domain.port.out;

import com.omnip.identity.domain.model.Users;

/**
 * Output port for user persistence in onboarding context.
 * Cross-context port — Onboarding needs to create/update users
 * owned by the Identity context.
 * Note: save() is inherited from JpaRepository/CrudRepository.
 */
public interface OnboardingUserPort {

    Users findByProviderUserId(String providerUserId);

    Users findByEmail(String email);

    Users save(Users user);
}
```

**Step 3: Fix StoreDomainService**

Edit `src/main/java/com/omnip/onboarding/domain/service/StoreDomainService.java`:

Replace import:
```java
import com.omnip.onboarding.adapter.out.persistence.StoreJpaRepository;
```
With:
```java
import com.omnip.onboarding.domain.port.out.StoreRepositoryPort;
```

Replace field/constructor:
- `StoreJpaRepository storeRepository` → `StoreRepositoryPort storeRepository`

**Step 4: Fix AdminOnboardingDomainService**

Edit `src/main/java/com/omnip/onboarding/domain/service/AdminOnboardingDomainService.java`:

Replace imports:
```java
import com.omnip.identity.adapter.out.keycloak.KeycloakAdminClientService;
import com.omnip.onboarding.adapter.out.persistence.StoreJpaRepository;
import com.omnip.identity.adapter.out.persistence.UserJpaRepository;
```
With:
```java
import com.omnip.onboarding.domain.port.out.KeycloakOrganizationPort;
import com.omnip.onboarding.domain.port.out.StoreRepositoryPort;
import com.omnip.onboarding.domain.port.out.OnboardingUserPort;
```

Replace fields/constructor:
- `KeycloakAdminClientService keycloakAdminClientService` → `KeycloakOrganizationPort keycloakAdminClientService`
- `StoreJpaRepository storeRepository` → `StoreRepositoryPort storeRepository`
- `UserJpaRepository usersRepository` → `OnboardingUserPort usersRepository`

**Step 5: Fix StoreOnboardingDomainService**

Same as above — apply same replacement for imports, fields, and constructor in `src/main/java/com/omnip/onboarding/domain/service/StoreOnboardingDomainService.java`.

**Step 6: Fix RegistrationHelper**

Edit `src/main/java/com/omnip/onboarding/domain/service/RegistrationHelper.java`:

Replace imports:
```java
import com.omnip.onboarding.adapter.out.persistence.StoreJpaRepository;
import com.omnip.identity.adapter.out.persistence.UserJpaRepository;
```
With:
```java
import com.omnip.onboarding.domain.port.out.StoreRepositoryPort;
import com.omnip.onboarding.domain.port.out.OnboardingUserPort;
```

Replace fields/constructor:
- `StoreJpaRepository storeRepository` → `StoreRepositoryPort storeRepository`
- `UserJpaRepository usersRepository` → `OnboardingUserPort usersRepository`

**Step 7: Verify build passes**

```bash
mvn compile -pl . -q 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`

**Step 8: Commit**

```bash
git add src/main/java/com/omnip/onboarding/
git commit -m "refactor(onboarding): inject port interfaces instead of JPA repos in domain services, fix cross-context violations"
```

---

## Task 4: Fix transaction ports and domain services

**Files:**
- Modify: `src/main/java/com/omnip/transaction/domain/port/out/TransactionRepositoryPort.java`
- Modify: `src/main/java/com/omnip/transaction/domain/port/out/StoreMutationRepositoryPort.java`
- Modify: `src/main/java/com/omnip/transaction/domain/port/out/StoreBalancePort.java`
- Modify: `src/main/java/com/omnip/transaction/domain/service/TransactionDomainService.java`
- Modify: `src/main/java/com/omnip/transaction/domain/service/BalanceDomainService.java`

**Step 1: Extend TransactionRepositoryPort**

`TransactionDomainService` calls `.save()`, `.existsByStoreIdAndProductDenomId...()` on `TransactionJpaRepository`. Add these.

Edit `src/main/java/com/omnip/transaction/domain/port/out/TransactionRepositoryPort.java`:

```java
package com.omnip.transaction.domain.port.out;

import com.omnip.transaction.domain.model.Transactions;
import com.omnip.transaction.domain.model.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Note: save() is inherited from JpaRepository/CrudRepository.
 */
public interface TransactionRepositoryPort {

    Transactions save(Transactions transaction);

    Optional<Transactions> findByIdAndStoreIdWithDetails(UUID id, UUID storeId);

    Page<Transactions> findByStoreIdWithDetails(UUID storeId, Pageable pageable);

    boolean existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
            UUID storeId, UUID denomId, String targetNumber,
            Collection<TransactionStatus> statuses,
            LocalDateTime since);
}
```

**Step 2: Extend StoreMutationRepositoryPort**

`BalanceDomainService` calls `.save()` on `StoreMutationJpaRepository`.

Edit `src/main/java/com/omnip/transaction/domain/port/out/StoreMutationRepositoryPort.java`:

```java
package com.omnip.transaction.domain.port.out;

import com.omnip.transaction.domain.model.StoreMutations;

/**
 * Note: save() is inherited from JpaRepository/CrudRepository.
 */
public interface StoreMutationRepositoryPort {

    StoreMutations save(StoreMutations mutation);
}
```

**Step 3: Extend StoreBalancePort**

`BalanceDomainService` also calls `.save()` and `.findById()` on `StoreJpaRepository`. Add to port.

Edit `src/main/java/com/omnip/transaction/domain/port/out/StoreBalancePort.java`:

```java
package com.omnip.transaction.domain.port.out;

import com.omnip.onboarding.domain.model.Stores;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for store balance operations — cross-context.
 * Transaction context needs read/write access to store balance data.
 * Note: save() and findById() are inherited from JpaRepository/CrudRepository.
 */
public interface StoreBalancePort {

    Optional<Stores> findById(UUID id);

    Stores save(Stores store);

    Optional<Stores> findByIdWithPessimisticLock(UUID id);
}
```

**Step 4: Fix BalanceDomainService**

Edit `src/main/java/com/omnip/transaction/domain/service/BalanceDomainService.java`:

Replace imports:
```java
import com.omnip.transaction.adapter.out.persistence.StoreMutationJpaRepository;
import com.omnip.onboarding.adapter.out.persistence.StoreJpaRepository;
```
With:
```java
import com.omnip.transaction.domain.port.out.StoreMutationRepositoryPort;
import com.omnip.transaction.domain.port.out.StoreBalancePort;
```

Replace fields/constructor:
- `StoreJpaRepository storeRepository` → `StoreBalancePort storeRepository`
- `StoreMutationJpaRepository storeMutationRepository` → `StoreMutationRepositoryPort storeMutationRepository`

**Step 5: Fix TransactionDomainService**

Edit `src/main/java/com/omnip/transaction/domain/service/TransactionDomainService.java`:

Replace imports:
```java
import com.omnip.catalog.adapter.out.persistence.DenomJpaRepository;
import com.omnip.onboarding.adapter.out.persistence.StoreJpaRepository;
import com.omnip.transaction.adapter.out.persistence.TransactionJpaRepository;
```
With:
```java
import com.omnip.catalog.domain.port.out.DenomRepositoryPort;
import com.omnip.transaction.domain.port.out.StoreBalancePort;
import com.omnip.transaction.domain.port.out.TransactionRepositoryPort;
```

Replace fields/constructor:
- `TransactionJpaRepository transactionRepository` → `TransactionRepositoryPort transactionRepository`
- `StoreJpaRepository storeRepository` → `StoreBalancePort storeRepository`
- `DenomJpaRepository productDenomRepository` → `DenomRepositoryPort productDenomRepository`

Note: `TransactionDomainService` calls `storeRepository.findById()` (inherited from JPA) — this is now covered by `StoreBalancePort.findById()` added in Step 3. It also calls `productDenomRepository.findById()` — covered by `DenomRepositoryPort.findById()` added in Task 1 Step 1.

**Step 6: Verify build passes**

```bash
mvn compile -pl . -q 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`

**Step 7: Commit**

```bash
git add src/main/java/com/omnip/transaction/
git commit -m "refactor(transaction): inject port interfaces instead of JPA repos in domain services"
```

---

## Task 5: Update integration test to mock ports instead of JPA repos

**Files:**
- Modify: `src/test/java/com/omnip/transaction/adapter/in/web/PurchaseFlowIntegrationTest.java`

**Context:**
After Task 4, `TransactionDomainService` and `BalanceDomainService` inject ports (`TransactionRepositoryPort`, `StoreBalancePort`, `DenomRepositoryPort`, `StoreMutationRepositoryPort`). The integration test currently mocks the concrete JPA repo classes — those mocks no longer wire correctly.

**Step 1: Update @MockitoBean targets in PurchaseFlowIntegrationTest**

Replace:
```java
@MockitoBean
private TransactionJpaRepository transactionRepository;

@MockitoBean
private StoreJpaRepository storeRepository;

@MockitoBean
private DenomJpaRepository productDenomRepository;

@MockitoBean
private StoreMutationJpaRepository storeMutationRepository;
```
With:
```java
@MockitoBean
private TransactionRepositoryPort transactionRepository;

@MockitoBean
private StoreBalancePort storeRepository;

@MockitoBean
private DenomRepositoryPort productDenomRepository;

@MockitoBean
private StoreMutationRepositoryPort storeMutationRepository;
```

Update imports accordingly — remove `adapter.out.persistence` imports, add `domain.port.out` imports.

**Step 2: Run all tests**

```bash
mvn test 2>&1 | tail -20
```
Expected: `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS`

**Step 3: Commit**

```bash
git add src/test/
git commit -m "refactor(test): mock port interfaces instead of JPA repos in PurchaseFlowIntegrationTest"
```

---

## Task 6: Final verification

**Step 1: Full build with tests**

```bash
cd /Users/kokohbudi/myProjects/omnip-services-3
mvn clean package -DskipTests=false 2>&1 | tail -10
```
Expected: `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS`

**Step 2: Verify no domain service imports from adapter layer**

```bash
grep -rn "import com.omnip.*adapter" src/main/java/com/omnip/*/domain/service/ | grep -v "^Binary"
```
Expected: zero results (or only DTOs from `adapter/in/web/dto` — those are acceptable)

**Step 3: Update Tasks.md**

Add entry in `## ✅ DONE` section:
```markdown
### Hexagonal Architecture Cleanup (2026-02-25)
- [x] All domain services inject port interfaces (not JPA repos)
- [x] Cross-context violations fixed (onboarding no longer imports identity.adapter)
- [x] TransactionRepositoryPort, StoreMutationRepositoryPort, StoreBalancePort extended
- [x] DenomRepositoryPort, StoreRepositoryPort, OnboardingUserPort, UserRepositoryPort extended
- [x] PurchaseFlowIntegrationTest updated to mock ports
- [x] All 15 tests pass
```

**Step 4: Commit**

```bash
git add Tasks.md
git commit -m "chore: update Tasks.md — hexagonal cleanup complete"
```
