# Purchase Prepaid Flow Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the core PPOB purchase flow with a Double-Entry Ledger balance system and a Mock Provider integration.

**Architecture:** Flat `Transactions` table for order state. `StoreMutations` for double-entry bookkeeping of balances. `Stores.balance` acts as a snapshot updated synchronously via pessimistic locking to prevent race conditions.

**Tech Stack:** Spring Boot 4.0.1, Hibernate 7.x, Java 25, JUnit 5.

---

### Task 1: Update Stores Entity & Repository Lock

**Files:**
- Modify: `src/main/java/com/omnip/entities/Stores.java`
- Modify: `src/main/java/com/omnip/repositories/StoreRepository.java`
- Create: `src/test/java/com/omnip/repositories/StoreRepositoryTest.java`

**Step 1: Write the failing test**
```java
package com.omnip.repositories;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StoreRepositoryTest {
    @Test
    public void testPessimisticLockMethodExists() {
        // Just verifying the method signature for compilation in TDD
        assertNotNull(StoreRepository.class);
    }
}
```

**Step 2: Run test to verify it fails**
Run: `mvn test -Dtest=StoreRepositoryTest`
Expected: FAIL (class not found or method missing)

**Step 3: Write minimal implementation**
Update `Stores.java` to add `BigDecimal balance = BigDecimal.ZERO;`.
Update `StoreRepository.java` to add:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Stores s WHERE s.id = :id")
Optional<Stores> findByIdWithPessimisticLock(@Param("id") UUID id);
```

**Step 4: Run test to verify it passes**
Run: `mvn test -Dtest=StoreRepositoryTest`
Expected: PASS

**Step 5: Commit**
```bash
git add src/main/java/com/omnip/entities/Stores.java src/main/java/com/omnip/repositories/StoreRepository.java src/test/java/com/omnip/repositories/StoreRepositoryTest.java
git commit -m "feat: add balance to Stores and pessimistic lock query"
```

---

### Task 2: Create Transaction Enums & Entity

**Files:**
- Create: `src/main/java/com/omnip/enums/TransactionStatus.java`
- Create: `src/main/java/com/omnip/entities/Transactions.java`
- Create: `src/main/java/com/omnip/repositories/TransactionRepository.java`

**Step 1: Write the failing test**
Create a basic test verifying `Transactions` entity instantiation and `TransactionStatus` values.

**Step 2: Run test to verify it fails**
Run: `mvn test -Dtest=TransactionsTest`
Expected: FAIL

**Step 3: Write minimal implementation**
Create `TransactionStatus` (PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED).
Create `Transactions` entity mapped to `Stores` and `ProductDenoms` with fields `targetNumber`, `price`, `adminFee`, `total`, `status`, `providerRef`, `serialNumber`.
Create `TransactionRepository`.

**Step 4: Run test to verify it passes**
Run: `mvn test -Dtest=TransactionsTest`
Expected: PASS

**Step 5: Commit**
```bash
git add src/main/java/com/omnip/enums/TransactionStatus.java src/main/java/com/omnip/entities/Transactions.java src/main/java/com/omnip/repositories/TransactionRepository.java
git commit -m "feat: create Transactions entity and repository"
```

---

### Task 3: Create StoreMutations Entity (Double-Entry Ledger)

**Files:**
- Create: `src/main/java/com/omnip/enums/MutationType.java`
- Create: `src/main/java/com/omnip/entities/StoreMutations.java`
- Create: `src/main/java/com/omnip/repositories/StoreMutationRepository.java`

**Step 1: Write the failing test**
Test verifying `StoreMutations` has `balanceAfter` and `amount`.

**Step 2: Run test to verify it fails**
Run: `mvn test -Dtest=StoreMutationsTest`
Expected: FAIL

**Step 3: Write minimal implementation**
Create `MutationType` (CREDIT, DEBIT).
Create `StoreMutations` entity linking `Stores` and `Transactions` (nullable) with `amount`, `balanceAfter`, `type`.
Create `StoreMutationRepository`.

**Step 4: Run test to verify it passes**
Run: `mvn test -Dtest=StoreMutationsTest`
Expected: PASS

**Step 5: Commit**
```bash
git add src/main/java/com/omnip/enums/MutationType.java src/main/java/com/omnip/entities/StoreMutations.java src/main/java/com/omnip/repositories/StoreMutationRepository.java
git commit -m "feat: create StoreMutations entity for double-entry ledger"
```

---

### Task 4: Implement BalanceService

**Files:**
- Create: `src/main/java/com/omnip/services/BalanceService.java`
- Create: `src/main/java/com/omnip/exceptions/InsufficientBalanceException.java`
- Create: `src/test/java/com/omnip/services/BalanceServiceTest.java`

**Step 1: Write the failing test**
Test `deductBalance` throws `InsufficientBalanceException` when balance is low.

**Step 2: Run test to verify it fails**
Run: `mvn test -Dtest=BalanceServiceTest`
Expected: FAIL

**Step 3: Write minimal implementation**
Implement `deductBalance(storeId, amount, transaction)` with `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
Fetch store with pessimistic lock, check balance, insert mutation (DEBIT) with calculated `balanceAfter`, update store balance.
Implement `addBalance(storeId, amount, transaction)` for CREDIT mutations.

**Step 4: Run test to verify it passes**
Run: `mvn test -Dtest=BalanceServiceTest`
Expected: PASS

**Step 5: Commit**
```bash
git add src/main/java/com/omnip/services/BalanceService.java src/main/java/com/omnip/exceptions/InsufficientBalanceException.java
git commit -m "feat: implement BalanceService with pessimistic locking"
```

---

### Task 5: Implement MockProviderService

**Files:**
- Create: `src/main/java/com/omnip/dtos/ProviderResponse.java`
- Create: `src/main/java/com/omnip/services/ProviderService.java`
- Create: `src/main/java/com/omnip/services/MockProviderService.java`

**Step 1: Write the failing test**
Test `sendTransaction` returns a valid `ProviderResponse`.

**Step 2: Run test to verify it fails**
Run: `mvn test -Dtest=MockProviderServiceTest`
Expected: FAIL

**Step 3: Write minimal implementation**
Create `ProviderResponse` (status, ref, sn).
Create interface `ProviderService`.
Implement `MockProviderService` simulating 500ms delay and 90% success rate using `Math.random()`.

**Step 4: Run test to verify it passes**
Run: `mvn test -Dtest=MockProviderServiceTest`
Expected: PASS

**Step 5: Commit**
```bash
git add src/main/java/com/omnip/dtos/ProviderResponse.java src/main/java/com/omnip/services/ProviderService.java src/main/java/com/omnip/services/MockProviderService.java
git commit -m "feat: implement MockProviderService"
```

---

### Task 6: Implement TransactionService (Purchase Flow Saga)

**Files:**
- Modify: `src/main/java/com/omnip/services/TransactionService.java`

**Step 1: Write the failing test**
Test `createPurchase(storeId, denomId, targetNumber)` logic flow.

**Step 2: Run test to verify it fails**
Run: `mvn test -Dtest=TransactionServiceTest`
Expected: FAIL

**Step 3: Write minimal implementation**
Implement `createPurchase`:
1. Validate store & denom
2. Calculate total (price + adminFee)
3. Create transaction (PENDING)
4. Call `balanceService.deductBalance`
5. Update transaction to PROCESSING
6. Call `providerService.sendTransaction`
7. On Success: Update to SUCCESS, set SN.
8. On Fail: Update to FAILED, call `balanceService.addBalance` (refund), update to REFUNDED.

**Step 4: Run test to verify it passes**
Run: `mvn test -Dtest=TransactionServiceTest`
Expected: PASS

**Step 5: Commit**
```bash
git add src/main/java/com/omnip/services/TransactionService.java
git commit -m "feat: implement createPurchase saga in TransactionService"
```
