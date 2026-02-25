# Hexagonal Architecture Cleanup — Design Document

> **Date**: 2026-02-25
> **Author**: August (PM) + Neo (Tech Lead)
> **Status**: Approved
> **Approach**: Port-First (Approach A)

---

## Problem Statement

Domain layer services currently import directly from adapter layer (JPA repositories), violating the core principle of Hexagonal Architecture: **domain must not depend on infrastructure**.

### Violations Found

```
transaction/domain/service/TransactionDomainService.java
  ❌ import catalog.adapter.out.persistence.DenomJpaRepository
  ❌ import onboarding.adapter.out.persistence.StoreJpaRepository
  ❌ import transaction.adapter.out.persistence.TransactionJpaRepository

transaction/domain/service/BalanceDomainService.java
  ❌ import onboarding.adapter.out.persistence.StoreJpaRepository
  ❌ import transaction.adapter.out.persistence.StoreMutationJpaRepository

catalog/domain/service/CategoryDomainService.java
  ❌ import catalog.adapter.out.persistence.CategoryJpaRepository

catalog/domain/service/ProductDomainService.java
  ❌ import catalog.adapter.out.persistence.ProductJpaRepository
  ❌ import catalog.adapter.out.persistence.CategoryJpaRepository

catalog/domain/service/DenomDomainService.java
  ❌ import catalog.adapter.out.persistence.DenomJpaRepository
  ❌ import catalog.adapter.out.persistence.DenomMetaJpaRepository
  ❌ import catalog.adapter.out.persistence.ProductJpaRepository

identity/domain/service/UserDomainService.java
  ❌ import identity.adapter.out.persistence.UserJpaRepository
  ❌ import identity.adapter.out.keycloak.KeycloakAdminClientService

identity/domain/service/UserManagementHelper.java
  ❌ import identity.adapter.out.persistence.UserJpaRepository

onboarding/domain/service/AdminOnboardingDomainService.java
  ❌ import onboarding.adapter.out.persistence.StoreJpaRepository
  ❌ import identity.adapter.out.persistence.UserJpaRepository  (cross-context!)

onboarding/domain/service/StoreDomainService.java
  ❌ import onboarding.adapter.out.persistence.StoreJpaRepository

onboarding/domain/service/StoreOnboardingDomainService.java
  ❌ import onboarding.adapter.out.persistence.StoreJpaRepository
  ❌ import identity.adapter.out.persistence.UserJpaRepository  (cross-context!)

onboarding/domain/service/RegistrationHelper.java
  ❌ import onboarding.adapter.out.persistence.StoreJpaRepository
  ❌ import identity.adapter.out.persistence.UserJpaRepository  (cross-context!)
```

---

## Solution: Port-First Approach

Domain services inject **port interfaces** (abstractions). JPA repositories implement those port interfaces directly — no intermediate adapter class needed.

### Target State

```
domain/service/TransactionDomainService.java
  ✅ inject TransactionRepositoryPort
  ✅ inject StoreBalancePort
  ✅ inject DenomRepositoryPort (cross-context port, catalog owns it)

domain/service/BalanceDomainService.java
  ✅ inject StoreBalancePort
  ✅ inject StoreMutationRepositoryPort
```

---

## Scope of Changes per Bounded Context

### catalog

| Domain Service | Currently Injects | After Cleanup |
|---|---|---|
| `CategoryDomainService` | `CategoryJpaRepository` | `CategoryRepositoryPort` |
| `ProductDomainService` | `ProductJpaRepository`, `CategoryJpaRepository` | `ProductRepositoryPort`, `CategoryRepositoryPort` |
| `DenomDomainService` | `DenomJpaRepository`, `DenomMetaJpaRepository`, `ProductJpaRepository` | `DenomRepositoryPort`, `DenomMetaRepositoryPort`, `ProductRepositoryPort` |

**Ports to extend:**
- `DenomRepositoryPort` — add `findById(UUID)` and `save(ProductDenoms)` (used by `TransactionDomainService`)
- `ProductRepositoryPort` — verify methods already cover `DenomDomainService` needs

### identity

| Domain Service | Currently Injects | After Cleanup |
|---|---|---|
| `UserDomainService` | `UserJpaRepository`, `KeycloakAdminClientService` | `UserRepositoryPort`, `KeycloakIdentityPort` |
| `UserManagementHelper` | `UserJpaRepository` | `UserRepositoryPort` |
| `IdentityDomainService` | `KeycloakAdminClientService` | `KeycloakIdentityPort` (already exists) |

**Ports to extend:**
- `UserRepositoryPort` — add `findById(UUID)`, `save(Users)`, `findAll()`, `findByStoreId(String)` as needed

### onboarding

| Domain Service | Currently Injects | After Cleanup |
|---|---|---|
| `StoreDomainService` | `StoreJpaRepository` | `StoreRepositoryPort` |
| `AdminOnboardingDomainService` | `StoreJpaRepository`, `UserJpaRepository` (cross-context!) | `StoreRepositoryPort`, `OnboardingUserPort` |
| `StoreOnboardingDomainService` | `StoreJpaRepository`, `UserJpaRepository` (cross-context!) | `StoreRepositoryPort`, `OnboardingUserPort` |
| `RegistrationHelper` | `StoreJpaRepository`, `UserJpaRepository` (cross-context!) | `StoreRepositoryPort`, `OnboardingUserPort` |

**Ports to extend:**
- `StoreRepositoryPort` — add `findById(UUID)`, `save(Stores)`, `findAll()` as needed
- `OnboardingUserPort` (already exists) — verify it covers all methods used by onboarding domain services

### transaction

| Domain Service | Currently Injects | After Cleanup |
|---|---|---|
| `TransactionDomainService` | `TransactionJpaRepository`, `StoreJpaRepository`, `DenomJpaRepository` | `TransactionRepositoryPort`, `StoreBalancePort`, `DenomRepositoryPort` |
| `BalanceDomainService` | `StoreJpaRepository`, `StoreMutationJpaRepository` | `StoreBalancePort`, `StoreMutationRepositoryPort` |

**Ports to extend:**
- `TransactionRepositoryPort` — add `save(Transactions)`, `findById(UUID)`, `existsByStoreIdAndProductDenomId...()`
- `StoreMutationRepositoryPort` — add `save(StoreMutations)`
- `StoreBalancePort` — add `findById(UUID)`, `save(Stores)` (currently only has `findByIdWithPessimisticLock`)

---

## What Is NOT Changed

- No new adapter classes — JPA repositories directly implement port interfaces
- No changes to business logic or behavior
- No changes to API contracts or response formats
- Integration test (`PurchaseFlowIntegrationTest`) updated: mock port interfaces instead of 4 JPA repos
- Build must pass `mvn clean package -DskipTests=false` at the end

---

## Dependency Rule (enforced after cleanup)

```
adapter/in/web  →  domain/port/in  (use cases)
domain/service  →  domain/port/out (output ports)
adapter/out     →  domain/port/out (implements ports)

FORBIDDEN after cleanup:
domain/service  →  adapter/out     ❌
domain/service  →  other_context/adapter/out ❌
```

---

## Testing Strategy

- All 15 existing tests must still pass
- `PurchaseFlowIntegrationTest`: update `@MockitoBean` targets from JPA repos to port interfaces
- `TransactionDomainServiceTest`: already mocks `ProviderPort` — no change needed
- No new tests required (behavior unchanged)
