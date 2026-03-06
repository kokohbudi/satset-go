package com.omnip.transaction.domain.service;

import com.omnip.onboarding.domain.model.Stores;
import com.omnip.shared.exception.InsufficientBalanceException;
import com.omnip.shared.exception.ResourceNotFoundException;
import com.omnip.transaction.domain.model.MutationReferenceType;
import com.omnip.transaction.domain.model.MutationType;
import com.omnip.transaction.domain.model.StoreMutations;
import com.omnip.transaction.domain.port.out.StoreBalancePort;
import com.omnip.transaction.domain.port.out.StoreMutationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BalanceDomainServiceTest {

    @Mock private StoreBalancePort storeRepository;
    @Mock private StoreMutationRepositoryPort storeMutationRepository;

    @InjectMocks
    private BalanceDomainService service;

    private UUID storeId;
    private Stores store;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        store = new Stores();
        store.setId(storeId);
        store.setBalance(new BigDecimal("100000"));

        when(storeMutationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ==================== deductBalance ====================

    @Test
    void deductBalance_Success_DeductsCorrectAmount() throws InsufficientBalanceException {
        when(storeRepository.findByIdWithPessimisticLock(storeId)).thenReturn(Optional.of(store));

        StoreMutations result = service.deductBalance(
                storeId, new BigDecimal("30000"),
                MutationReferenceType.PURCHASE, UUID.randomUUID(), "Pulsa");

        assertEquals(new BigDecimal("70000"), store.getBalance());
        assertEquals(MutationType.DEBIT, result.getType());
        assertEquals(new BigDecimal("30000"), result.getAmount());
        assertEquals(new BigDecimal("70000"), result.getBalanceAfter());
    }

    @Test
    void deductBalance_ExactBalance_SucceedsWithZeroRemaining() throws InsufficientBalanceException {
        when(storeRepository.findByIdWithPessimisticLock(storeId)).thenReturn(Optional.of(store));

        service.deductBalance(storeId, new BigDecimal("100000"),
                MutationReferenceType.PURCHASE, UUID.randomUUID(), "Exact balance");

        assertEquals(BigDecimal.ZERO, store.getBalance());
    }

    @Test
    void deductBalance_InsufficientBalance_ThrowsException() {
        when(storeRepository.findByIdWithPessimisticLock(storeId)).thenReturn(Optional.of(store));

        assertThrows(InsufficientBalanceException.class,
                () -> service.deductBalance(storeId, new BigDecimal("100001"),
                        MutationReferenceType.PURCHASE, UUID.randomUUID(), "Over limit"));

        verify(storeMutationRepository, never()).save(any());
        verify(storeRepository, never()).save(any());
    }

    @Test
    void deductBalance_StoreNotFound_ThrowsResourceNotFoundException() {
        when(storeRepository.findByIdWithPessimisticLock(storeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.deductBalance(storeId, new BigDecimal("10000"),
                        MutationReferenceType.PURCHASE, UUID.randomUUID(), "desc"));
    }

    @Test
    void deductBalance_SavesMutationWithCorrectFields() throws InsufficientBalanceException {
        UUID refId = UUID.randomUUID();
        when(storeRepository.findByIdWithPessimisticLock(storeId)).thenReturn(Optional.of(store));

        service.deductBalance(storeId, new BigDecimal("20000"),
                MutationReferenceType.PURCHASE, refId, "Pulsa Telkomsel");

        ArgumentCaptor<StoreMutations> captor = ArgumentCaptor.forClass(StoreMutations.class);
        verify(storeMutationRepository).save(captor.capture());
        StoreMutations saved = captor.getValue();

        assertEquals(store.getId(), saved.getStoreId());
        assertEquals(MutationReferenceType.PURCHASE, saved.getReferenceType());
        assertEquals(refId, saved.getReferenceId());
        assertEquals("Pulsa Telkomsel", saved.getDescription());
    }

    // ==================== addBalance ====================

    @Test
    void addBalance_Success_AddsCorrectAmount() {
        when(storeRepository.findByIdWithPessimisticLock(storeId)).thenReturn(Optional.of(store));

        StoreMutations result = service.addBalance(
                storeId, new BigDecimal("50000"),
                MutationReferenceType.TOP_UP, UUID.randomUUID(), "Top-up");

        assertEquals(new BigDecimal("150000"), store.getBalance());
        assertEquals(MutationType.CREDIT, result.getType());
        assertEquals(new BigDecimal("50000"), result.getAmount());
        assertEquals(new BigDecimal("150000"), result.getBalanceAfter());
    }

    @Test
    void addBalance_StoreNotFound_ThrowsResourceNotFoundException() {
        when(storeRepository.findByIdWithPessimisticLock(storeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.addBalance(storeId, new BigDecimal("50000"),
                        MutationReferenceType.TOP_UP, UUID.randomUUID(), "desc"));
    }

    @Test
    void addBalance_RefundScenario_SavesMutationWithRefundType() {
        UUID txId = UUID.randomUUID();
        when(storeRepository.findByIdWithPessimisticLock(storeId)).thenReturn(Optional.of(store));

        service.addBalance(storeId, new BigDecimal("10000"),
                MutationReferenceType.REFUND, txId, "Refund transaksi gagal");

        ArgumentCaptor<StoreMutations> captor = ArgumentCaptor.forClass(StoreMutations.class);
        verify(storeMutationRepository).save(captor.capture());
        assertEquals(MutationReferenceType.REFUND, captor.getValue().getReferenceType());
        assertEquals(txId, captor.getValue().getReferenceId());
    }

    // ==================== getBalance ====================

    @Test
    void getBalance_Found_ReturnsBalance() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        BigDecimal balance = service.getBalance(storeId);

        assertEquals(new BigDecimal("100000"), balance);
    }

    @Test
    void getBalance_StoreNotFound_ThrowsResourceNotFoundException() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getBalance(storeId));
    }
}
