# Hexagonal Architecture Refactoring — Remaining Tasks

> **Status**: Phase 1 (P3) COMPLETE | Phase 2 (P2) PENDING | Phase 3 (P1) PENDING
> **Last Updated**: 2026-03-06

---

## ✅ Phase 1 (P3): Shared Imports Domain — COMPLETE

**Completed:** 2026-03-06

### Changes Made
- Created `RoleInfo.java` and `GroupInfo.java` shared DTOs
- Created `IdentityMapper.java` for domain → shared DTO conversion
- Updated all shared layer components to use DTOs instead of domain models
- Added port methods returning DTOs: `getMenuRoleInfos()`, `findByEmailDTO()`, `findStoreIdByProviderUserId()`
- All tests passing (372 tests, 3 unrelated failures)

### Deferred
- `DataSeeder.java` — Infrastructure component, acceptable exception

---

## 🟡 Phase 2 (P2): Cross-Context FK Coupling — PENDING

### Problem
Domain models have `@ManyToOne` relationships across bounded contexts:

| Entity | Field | Problem |
|--------|-------|---------|
| `Products.java` | `@ManyToOne Categories category` | catalog → catalog (OK, same context) |
| `ProductDenoms.java` | `@ManyToOne Products product` | catalog → catalog (OK, same context) |
| `ProductDenomMeta.java` | `@ManyToOne ProductDenoms productDenom` | catalog → catalog (OK, same context) |

**Note:** After analysis, the `@ManyToOne` relationships are within the same bounded context (catalog). The cross-context references already use UUID:

| Entity | Field | Status |
|--------|-------|--------|
| `Transactions.storeId` | `UUID` | ✅ Already correct |
| `Transactions.productDenomId` | `UUID` | ✅ Already correct |
| `Users.storeId` | `UUID` | ✅ Already correct |
| `StoreMutations.storeId` | `UUID` | ✅ Already correct |

### Decision
**P2 is already compliant.** Cross-context references use UUID, not JPA relationships.

---

## 🔴 Phase 3 (P1): Domain Models Coupled to JPA — PENDING

### Problem
All domain models in `domain/model/` are JPA `@Entity` objects with persistence annotations.

### Target Architecture

```
domain/model/                    ← Pure POJOs (NO JPA)
├── Store.java
├── User.java
├── Transaction.java
└── ...

adapter/out/persistence/
├── entity/                      ← JPA Entities
│   ├── StoreJpaEntity.java
│   ├── UserJpaEntity.java
│   └── ...
├── mapper/                      ← Domain ↔ Entity
│   ├── StoreMapper.java
│   └── ...
└── repository/
    ├── StoreJpaRepository.java
    └── StoreRepositoryAdapter.java
```

### Migration Steps

#### Step 1: Create Pure Domain Models (POJO)
- Remove all JPA annotations from domain models
- Replace `@ManyToOne` with `UUID` references
- Keep business logic methods

#### Step 2: Create JPA Entities in Adapter Layer
- Copy current domain models to `adapter/out/persistence/entity/`
- Rename: `Stores` → `StoreJpaEntity`, `Users` → `UserJpaEntity`, etc.
- Keep JPA annotations

#### Step 3: Create Mappers
- `StoreMapper.java`: `Store` ↔ `StoreJpaEntity`
- `UserMapper.java`: `User` ↔ `UserJpaEntity`
- etc.

#### Step 4: Update Repository Adapters
- Inject `JpaRepository` and `Mapper`
- Convert domain → entity before save
- Convert entity → domain after find

### Files to Create

| Action | File Path |
|--------|-----------|
| CREATE | `adapter/out/persistence/entity/StoreJpaEntity.java` |
| CREATE | `adapter/out/persistence/entity/UserJpaEntity.java` |
| CREATE | `adapter/out/persistence/entity/TransactionJpaEntity.java` |
| CREATE | `adapter/out/persistence/entity/StoreMutationJpaEntity.java` |
| CREATE | `adapter/out/persistence/entity/WalletAccountJpaEntity.java` |
| CREATE | `adapter/out/persistence/entity/CategoryJpaEntity.java` |
| CREATE | `adapter/out/persistence/entity/ProductJpaEntity.java` |
| CREATE | `adapter/out/persistence/entity/ProductDenomJpaEntity.java` |
| CREATE | `adapter/out/persistence/entity/ProductDenomMetaJpaEntity.java` |
| CREATE | `adapter/out/persistence/mapper/StoreMapper.java` |
| CREATE | `adapter/out/persistence/mapper/UserMapper.java` |
| CREATE | `adapter/out/persistence/mapper/TransactionMapper.java` |
| CREATE | `adapter/out/persistence/mapper/StoreMutationMapper.java` |
| CREATE | `adapter/out/persistence/mapper/WalletAccountMapper.java` |
| CREATE | `adapter/out/persistence/mapper/CategoryMapper.java` |
| CREATE | `adapter/out/persistence/mapper/ProductMapper.java` |
| CREATE | `adapter/out/persistence/mapper/ProductDenomMapper.java` |
| CREATE | `adapter/out/persistence/mapper/ProductDenomMetaMapper.java` |

### Files to Modify

| Action | File | Change |
|--------|------|--------|
| MODIFY | `domain/model/Stores.java` | Remove JPA, rename to `Store.java` |
| MODIFY | `domain/model/Users.java` | Remove JPA, rename to `User.java` |
| MODIFY | `domain/model/Transactions.java` | Remove JPA, rename to `Transaction.java` |
| MODIFY | `domain/model/StoreMutations.java` | Remove JPA, rename to `StoreMutation.java` |
| MODIFY | `domain/model/WalletAccount.java` | Remove JPA |
| MODIFY | `domain/model/Categories.java` | Remove JPA, rename to `Category.java` |
| MODIFY | `domain/model/Products.java` | Remove JPA, rename to `Product.java` |
| MODIFY | `domain/model/ProductDenoms.java` | Remove JPA, rename to `ProductDenom.java` |
| MODIFY | `domain/model/ProductDenomMeta.java` | Remove JPA |
| MODIFY | All Repository Adapters | Use mappers |
| MODIFY | All Domain Services | Use new domain models |

### Estimated Effort
- **Duration:** 5-7 days
- **Risk:** Medium (large refactoring, many files)
- **Testing:** Extensive regression testing required

### Execution Order
1. Start with simple entities: `Category`, `ProductDenomMeta`
2. Move to medium: `Product`, `ProductDenom`
3. End with complex: `Store`, `User`, `Transaction`, `WalletAccount`
4. Run full test suite after each entity migration

---

## 📊 Progress Summary

| Phase | Description | Status | Effort |
|-------|-------------|--------|--------|
| P3 | Shared Imports Domain | ✅ COMPLETE | 1-2 days |
| P2 | Cross-Context FK | ✅ N/A (already compliant) | — |
| P1 | Domain/JPA Separation | 🔴 PENDING | 5-7 days |

---

## 🚀 How to Start Phase 3

```bash
# 1. Create a feature branch
git checkout -b refactor/hexagonal-p1-domain-jpa-separation

# 2. Start with Category (simplest entity)
# - Create CategoryJpaEntity.java
# - Create CategoryMapper.java
# - Update CategoryRepositoryAdapter.java
# - Remove JPA from Category.java
# - Run tests

# 3. Repeat for each entity
```

---

## ⚠️ Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Breaking existing functionality | Run full test suite after each entity |
| Missing mapper conversions | Write unit tests for each mapper |
| Performance regression | Benchmark critical paths before/after |
| Merge conflicts | Keep PRs small, one entity at a time |

---

## 📝 Notes

- **DataSeeder.java** remains an exception (infrastructure component)
- Consider using MapStruct for mappers if manual mapping becomes tedious
- Virtual threads benefit remains unchanged after refactoring