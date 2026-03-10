package com.satset.transaction.domain.port.in;

import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.transaction.domain.model.MutationReferenceType;
import com.satset.transaction.domain.model.MutationResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BalanceManagementUseCaseTest {

    @Test
    void testDeductBalanceReturnsMutationResult() throws InsufficientBalanceException {
        // Given
        BalanceManagementUseCase useCase = Mockito.mock(BalanceManagementUseCase.class);
        String walletId = "7001234567";
        BigDecimal amount = new BigDecimal("10000");
        MutationReferenceType referenceType = MutationReferenceType.PURCHASE;
        UUID referenceId = UUID.randomUUID();
        String description = "Test deduction";
        MutationResult expectedResult = new MutationResult(UUID.randomUUID(), new BigDecimal("90000"));

        // When
        when(useCase.deductBalance(walletId, amount, referenceType, referenceId, description))
            .thenReturn(expectedResult);
        MutationResult result = useCase.deductBalance(walletId, amount, referenceType, referenceId, description);

        // Then
        verify(useCase).deductBalance(walletId, amount, referenceType, referenceId, description);
        assertEquals(expectedResult.mutationId(), result.mutationId());
        assertEquals(expectedResult.balanceAfter(), result.balanceAfter());
    }

    @Test
    void testAddBalanceReturnsMutationResult() {
        // Given
        BalanceManagementUseCase useCase = Mockito.mock(BalanceManagementUseCase.class);
        String walletId = "7001234567";
        BigDecimal amount = new BigDecimal("10000");
        MutationReferenceType referenceType = MutationReferenceType.TOP_UP;
        UUID referenceId = UUID.randomUUID();
        String description = "Test addition";
        MutationResult expectedResult = new MutationResult(UUID.randomUUID(), new BigDecimal("110000"));

        // When
        when(useCase.addBalance(walletId, amount, referenceType, referenceId, description))
            .thenReturn(expectedResult);
        MutationResult result = useCase.addBalance(walletId, amount, referenceType, referenceId, description);

        // Then
        verify(useCase).addBalance(walletId, amount, referenceType, referenceId, description);
        assertEquals(expectedResult.mutationId(), result.mutationId());
        assertEquals(expectedResult.balanceAfter(), result.balanceAfter());
    }

    @Test
    void testGetBalanceReturnsBigDecimal() {
        // Given
        BalanceManagementUseCase useCase = Mockito.mock(BalanceManagementUseCase.class);
        String walletId = "7001234567";
        BigDecimal expectedBalance = new BigDecimal("100000");

        // When
        when(useCase.getBalance(walletId)).thenReturn(expectedBalance);
        BigDecimal result = useCase.getBalance(walletId);

        // Then
        verify(useCase).getBalance(walletId);
        assertEquals(expectedBalance, result);
    }
}
