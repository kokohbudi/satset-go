package com.satset.transaction.domain.service;

import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.transaction.domain.model.MutationReferenceType;
import com.satset.transaction.domain.model.MutationResult;
import com.satset.transaction.domain.model.StoreMutations;
import com.satset.transaction.domain.model.WalletAccount;
import com.satset.transaction.domain.port.out.StoreMutationRepositoryPort;
import com.satset.transaction.domain.port.out.WalletAccountPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceDomainServiceTest {

    @Mock
    private WalletAccountPort walletAccountPort;

    @Mock
    private StoreMutationRepositoryPort storeMutationRepository;

    private BalanceDomainService balanceDomainService;

    @BeforeEach
    void setUp() {
        balanceDomainService = new BalanceDomainService(walletAccountPort, storeMutationRepository);
    }

    @Test
    void testDeductBalanceSuccess() throws InsufficientBalanceException {
        // Given
        String walletId = "7001234567";
        BigDecimal amount = new BigDecimal("10000");
        MutationReferenceType referenceType = MutationReferenceType.PURCHASE;
        UUID referenceId = UUID.randomUUID();
        String description = "Test deduction";

        WalletAccount walletAccount = new WalletAccount(walletId, new BigDecimal("50000"));
        StoreMutations storeMutation = new StoreMutations();
        storeMutation.setWalletId(walletId);
        storeMutation.setBalanceAfter(new BigDecimal("40000"));
        storeMutation.setId(UUID.randomUUID());

        when(walletAccountPort.findByWalletIdWithLock(walletId)).thenReturn(Optional.of(walletAccount));
        when(storeMutationRepository.save(any(StoreMutations.class))).thenAnswer(invocation -> {
            StoreMutations argument = invocation.getArgument(0);
            if (argument.getId() == null) {
                argument.setId(UUID.randomUUID());
            }
            return argument;
        });
        when(walletAccountPort.save(any(WalletAccount.class))).thenReturn(walletAccount);

        // When
        MutationResult result = balanceDomainService.deductBalance(walletId, amount, referenceType, referenceId, description);

        // Then
        assertNotNull(result);
        assertNotNull(result.mutationId());
        assertEquals(new BigDecimal("40000"), result.balanceAfter());

        verify(walletAccountPort).findByWalletIdWithLock(walletId);
        verify(storeMutationRepository).save(any(StoreMutations.class));
        verify(walletAccountPort).save(any(WalletAccount.class));
    }

    @Test
    void testDeductBalanceInsufficientFunds() {
        // Given
        String walletId = "7001234567";
        BigDecimal amount = new BigDecimal("10000");
        MutationReferenceType referenceType = MutationReferenceType.PURCHASE;
        UUID referenceId = UUID.randomUUID();
        String description = "Test deduction";

        WalletAccount walletAccount = new WalletAccount(walletId, new BigDecimal("5000")); // Less than amount

        when(walletAccountPort.findByWalletIdWithLock(walletId)).thenReturn(Optional.of(walletAccount));

        // When & Then
        InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class, () -> {
            balanceDomainService.deductBalance(walletId, amount, referenceType, referenceId, description);
        });

        assertTrue(exception.getMessage().contains("Saldo tidak mencukupi"));
        verify(walletAccountPort).findByWalletIdWithLock(walletId);
        verify(storeMutationRepository, never()).save(any(StoreMutations.class));
        verify(walletAccountPort, never()).save(any(WalletAccount.class));
    }

    @Test
    void testDeductBalanceWalletAccountNotFound() {
        // Given
        String walletId = "7001234567";
        BigDecimal amount = new BigDecimal("10000");
        MutationReferenceType referenceType = MutationReferenceType.PURCHASE;
        UUID referenceId = UUID.randomUUID();
        String description = "Test deduction";

        when(walletAccountPort.findByWalletIdWithLock(walletId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            balanceDomainService.deductBalance(walletId, amount, referenceType, referenceId, description);
        });

        assertTrue(exception.getMessage().contains("WalletAccount"));
        verify(walletAccountPort).findByWalletIdWithLock(walletId);
        verify(storeMutationRepository, never()).save(any(StoreMutations.class));
        verify(walletAccountPort, never()).save(any(WalletAccount.class));
    }

    @Test
    void testAddBalanceSuccess() {
        // Given
        String walletId = "7001234567";
        BigDecimal amount = new BigDecimal("10000");
        MutationReferenceType referenceType = MutationReferenceType.TOP_UP;
        UUID referenceId = UUID.randomUUID();
        String description = "Test addition";

        WalletAccount walletAccount = new WalletAccount(walletId, new BigDecimal("40000"));
        StoreMutations storeMutation = new StoreMutations();
        storeMutation.setWalletId(walletId);
        storeMutation.setBalanceAfter(new BigDecimal("50000"));
        storeMutation.setId(UUID.randomUUID());

        when(walletAccountPort.findByWalletIdWithLock(walletId)).thenReturn(Optional.of(walletAccount));
        when(storeMutationRepository.save(any(StoreMutations.class))).thenAnswer(invocation -> {
            StoreMutations argument = invocation.getArgument(0);
            if (argument.getId() == null) {
                argument.setId(UUID.randomUUID());
            }
            return argument;
        });
        when(walletAccountPort.save(any(WalletAccount.class))).thenReturn(walletAccount);

        // When
        MutationResult result = balanceDomainService.addBalance(walletId, amount, referenceType, referenceId, description);

        // Then
        assertNotNull(result);
        assertNotNull(result.mutationId());
        assertEquals(new BigDecimal("50000"), result.balanceAfter());

        verify(walletAccountPort).findByWalletIdWithLock(walletId);
        verify(storeMutationRepository).save(any(StoreMutations.class));
        verify(walletAccountPort).save(any(WalletAccount.class));
    }

    @Test
    void testAddBalanceWalletAccountNotFound() {
        // Given
        String walletId = "7001234567";
        BigDecimal amount = new BigDecimal("10000");
        MutationReferenceType referenceType = MutationReferenceType.TOP_UP;
        UUID referenceId = UUID.randomUUID();
        String description = "Test addition";

        when(walletAccountPort.findByWalletIdWithLock(walletId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            balanceDomainService.addBalance(walletId, amount, referenceType, referenceId, description);
        });

        assertTrue(exception.getMessage().contains("WalletAccount"));
        verify(walletAccountPort).findByWalletIdWithLock(walletId);
        verify(storeMutationRepository, never()).save(any(StoreMutations.class));
        verify(walletAccountPort, never()).save(any(WalletAccount.class));
    }

    @Test
    void testGetBalanceSuccess() {
        // Given
        String walletId = "7001234567";
        BigDecimal expectedBalance = new BigDecimal("50000");

        WalletAccount walletAccount = new WalletAccount(walletId, expectedBalance);

        when(walletAccountPort.findByWalletId(walletId)).thenReturn(Optional.of(walletAccount));

        // When
        BigDecimal result = balanceDomainService.getBalance(walletId);

        // Then
        assertEquals(expectedBalance, result);
        verify(walletAccountPort).findByWalletId(walletId);
    }

    @Test
    void testGetBalanceWalletAccountNotFound() {
        // Given
        String walletId = "7001234567";

        when(walletAccountPort.findByWalletId(walletId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            balanceDomainService.getBalance(walletId);
        });

        assertTrue(exception.getMessage().contains("WalletAccount"));
        verify(walletAccountPort).findByWalletId(walletId);
    }
}
