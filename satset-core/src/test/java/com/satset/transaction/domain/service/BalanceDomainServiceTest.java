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
        UUID storeId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10000");
        MutationReferenceType referenceType = MutationReferenceType.PURCHASE;
        UUID referenceId = UUID.randomUUID();
        String description = "Test deduction";
        
        WalletAccount walletAccount = new WalletAccount(storeId, new BigDecimal("50000"));
        StoreMutations storeMutation = new StoreMutations();
        storeMutation.setStoreId(storeId);
        storeMutation.setBalanceAfter(new BigDecimal("40000"));
        storeMutation.setId(UUID.randomUUID());
        
        when(walletAccountPort.findByStoreIdWithLock(storeId)).thenReturn(Optional.of(walletAccount));
        when(storeMutationRepository.save(any(StoreMutations.class))).thenAnswer(invocation -> {
            StoreMutations argument = invocation.getArgument(0);
            if (argument.getId() == null) {
                argument.setId(UUID.randomUUID()); // Simulasikan ID yang di-generate oleh DB
            }
            return argument;
        });
        when(walletAccountPort.save(any(WalletAccount.class))).thenReturn(walletAccount);

        // When
        MutationResult result = balanceDomainService.deductBalance(storeId, amount, referenceType, referenceId, description);

        // Then
        assertNotNull(result);
        assertNotNull(result.mutationId());
        assertEquals(new BigDecimal("40000"), result.balanceAfter());
        
        verify(walletAccountPort).findByStoreIdWithLock(storeId);
        verify(storeMutationRepository).save(any(StoreMutations.class));
        verify(walletAccountPort).save(any(WalletAccount.class));
    }

    @Test
    void testDeductBalanceInsufficientFunds() {
        // Given
        UUID storeId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10000");
        MutationReferenceType referenceType = MutationReferenceType.PURCHASE;
        UUID referenceId = UUID.randomUUID();
        String description = "Test deduction";
        
        WalletAccount walletAccount = new WalletAccount(storeId, new BigDecimal("5000")); // Less than amount
        
        when(walletAccountPort.findByStoreIdWithLock(storeId)).thenReturn(Optional.of(walletAccount));

        // When & Then
        InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class, () -> {
            balanceDomainService.deductBalance(storeId, amount, referenceType, referenceId, description);
        });
        
        assertTrue(exception.getMessage().contains("Saldo tidak mencukupi"));
        verify(walletAccountPort).findByStoreIdWithLock(storeId);
        verify(storeMutationRepository, never()).save(any(StoreMutations.class));
        verify(walletAccountPort, never()).save(any(WalletAccount.class));
    }

    @Test
    void testDeductBalanceWalletAccountNotFound() {
        // Given
        UUID storeId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10000");
        MutationReferenceType referenceType = MutationReferenceType.PURCHASE;
        UUID referenceId = UUID.randomUUID();
        String description = "Test deduction";
        
        when(walletAccountPort.findByStoreIdWithLock(storeId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            balanceDomainService.deductBalance(storeId, amount, referenceType, referenceId, description);
        });
        
        assertTrue(exception.getMessage().contains("WalletAccount"));
        verify(walletAccountPort).findByStoreIdWithLock(storeId);
        verify(storeMutationRepository, never()).save(any(StoreMutations.class));
        verify(walletAccountPort, never()).save(any(WalletAccount.class));
    }

    @Test
    void testAddBalanceSuccess() {
        // Given
        UUID storeId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10000");
        MutationReferenceType referenceType = MutationReferenceType.TOP_UP;
        UUID referenceId = UUID.randomUUID();
        String description = "Test addition";
        
        WalletAccount walletAccount = new WalletAccount(storeId, new BigDecimal("40000"));
        StoreMutations storeMutation = new StoreMutations();
        storeMutation.setStoreId(storeId);
        storeMutation.setBalanceAfter(new BigDecimal("50000"));
        storeMutation.setId(UUID.randomUUID());
        
        when(walletAccountPort.findByStoreIdWithLock(storeId)).thenReturn(Optional.of(walletAccount));
        when(storeMutationRepository.save(any(StoreMutations.class))).thenAnswer(invocation -> {
            StoreMutations argument = invocation.getArgument(0);
            if (argument.getId() == null) {
                argument.setId(UUID.randomUUID()); // Simulasikan ID yang di-generate oleh DB
            }
            return argument;
        });
        when(walletAccountPort.save(any(WalletAccount.class))).thenReturn(walletAccount);

        // When
        MutationResult result = balanceDomainService.addBalance(storeId, amount, referenceType, referenceId, description);

        // Then
        assertNotNull(result);
        assertNotNull(result.mutationId());
        assertEquals(new BigDecimal("50000"), result.balanceAfter());
        
        verify(walletAccountPort).findByStoreIdWithLock(storeId);
        verify(storeMutationRepository).save(any(StoreMutations.class));
        verify(walletAccountPort).save(any(WalletAccount.class));
    }

    @Test
    void testAddBalanceWalletAccountNotFound() {
        // Given
        UUID storeId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10000");
        MutationReferenceType referenceType = MutationReferenceType.TOP_UP;
        UUID referenceId = UUID.randomUUID();
        String description = "Test addition";
        
        when(walletAccountPort.findByStoreIdWithLock(storeId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            balanceDomainService.addBalance(storeId, amount, referenceType, referenceId, description);
        });
        
        assertTrue(exception.getMessage().contains("WalletAccount"));
        verify(walletAccountPort).findByStoreIdWithLock(storeId);
        verify(storeMutationRepository, never()).save(any(StoreMutations.class));
        verify(walletAccountPort, never()).save(any(WalletAccount.class));
    }

    @Test
    void testGetBalanceSuccess() {
        // Given
        UUID storeId = UUID.randomUUID();
        BigDecimal expectedBalance = new BigDecimal("50000");
        
        WalletAccount walletAccount = new WalletAccount(storeId, expectedBalance);
        
        when(walletAccountPort.findByStoreId(storeId)).thenReturn(Optional.of(walletAccount));

        // When
        BigDecimal result = balanceDomainService.getBalance(storeId);

        // Then
        assertEquals(expectedBalance, result);
        verify(walletAccountPort).findByStoreId(storeId);
    }

    @Test
    void testGetBalanceWalletAccountNotFound() {
        // Given
        UUID storeId = UUID.randomUUID();
        
        when(walletAccountPort.findByStoreId(storeId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            balanceDomainService.getBalance(storeId);
        });
        
        assertTrue(exception.getMessage().contains("WalletAccount"));
        verify(walletAccountPort).findByStoreId(storeId);
    }
}