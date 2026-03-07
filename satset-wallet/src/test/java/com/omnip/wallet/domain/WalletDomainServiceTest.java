package com.satset.wallet.domain;

import com.satset.wallet.domain.model.MutationReferenceType;
import com.satset.wallet.domain.model.MutationType;
import com.satset.wallet.domain.model.WalletAccount;
import com.satset.wallet.domain.model.WalletMutation;
import com.satset.wallet.domain.port.out.WalletAccountPort;
import com.satset.wallet.domain.port.out.WalletMutationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletDomainServiceTest {

    @Mock private WalletAccountPort walletAccountPort;
    @Mock private WalletMutationPort walletMutationPort;

    private WalletDomainService service;
    private UUID storeId;
    private UUID referenceId;

    @BeforeEach
    void setUp() {
        service = new WalletDomainService(walletAccountPort, walletMutationPort);
        storeId = UUID.randomUUID();
        referenceId = UUID.randomUUID();
    }

    // --- getBalance ---

    @Test
    void getBalance_whenAccountExists_returnsBalance() {
        var account = new WalletAccount(UUID.randomUUID(), storeId, new BigDecimal("100000.0000"), 0L);
        when(walletAccountPort.findByStoreId(storeId)).thenReturn(Optional.of(account));

        assertThat(service.getBalance(storeId)).isEqualByComparingTo(new BigDecimal("100000.0000"));
    }

    @Test
    void getBalance_whenAccountDoesNotExist_throwsException() {
        when(walletAccountPort.findByStoreId(storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBalance(storeId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- debit ---

    @Test
    void debit_whenSufficientBalance_deductsAndCreatesMutation() {
        var account = new WalletAccount(UUID.randomUUID(), storeId, new BigDecimal("100000.0000"), 0L);
        when(walletMutationPort.findByReferenceIdAndReferenceType(referenceId, MutationReferenceType.TRANSACTION))
                .thenReturn(Optional.empty());
        when(walletAccountPort.findByStoreIdWithLock(storeId)).thenReturn(Optional.of(account));
        when(walletAccountPort.save(any())).thenAnswer(i -> i.getArgument(0));
        when(walletMutationPort.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.debit(storeId, new BigDecimal("50000.0000"), referenceId,
                MutationReferenceType.TRANSACTION, "Test purchase");

        assertThat(result.newBalance()).isEqualByComparingTo(new BigDecimal("50000.0000"));

        ArgumentCaptor<WalletMutation> captor = ArgumentCaptor.forClass(WalletMutation.class);
        verify(walletMutationPort).save(captor.capture());
        assertThat(captor.getValue().mutationType()).isEqualTo(MutationType.DEBIT);
        assertThat(captor.getValue().amount()).isEqualByComparingTo(new BigDecimal("50000.0000"));
        assertThat(captor.getValue().referenceId()).isEqualTo(referenceId);
    }

    @Test
    void debit_whenInsufficientBalance_throwsException() {
        var account = new WalletAccount(UUID.randomUUID(), storeId, new BigDecimal("10000.0000"), 0L);
        when(walletMutationPort.findByReferenceIdAndReferenceType(referenceId, MutationReferenceType.TRANSACTION))
                .thenReturn(Optional.empty());
        when(walletAccountPort.findByStoreIdWithLock(storeId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.debit(storeId, new BigDecimal("50000.0000"), referenceId,
                MutationReferenceType.TRANSACTION, "Test"))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(walletMutationPort, never()).save(any());
    }

    @Test
    void debit_whenDuplicate_returnsExistingResult() {
        UUID existingMutationId = UUID.randomUUID();
        var existingMutation = new WalletMutation(existingMutationId, storeId, new BigDecimal("50000"),
                MutationType.DEBIT, new BigDecimal("50000"), MutationReferenceType.TRANSACTION,
                referenceId, "existing", null);
        when(walletMutationPort.findByReferenceIdAndReferenceType(referenceId, MutationReferenceType.TRANSACTION))
                .thenReturn(Optional.of(existingMutation));

        var result = service.debit(storeId, new BigDecimal("50000"), referenceId,
                MutationReferenceType.TRANSACTION, "duplicate");

        assertThat(result.mutationId()).isEqualTo(existingMutationId);
        verify(walletAccountPort, never()).findByStoreIdWithLock(any());
        verify(walletMutationPort, never()).save(any());
    }

    @Test
    void debit_whenAccountNotFound_throwsException() {
        when(walletMutationPort.findByReferenceIdAndReferenceType(any(), any())).thenReturn(Optional.empty());
        when(walletAccountPort.findByStoreIdWithLock(storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.debit(storeId, new BigDecimal("100"), referenceId,
                MutationReferenceType.TRANSACTION, "test"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- credit ---

    @Test
    void credit_whenAccountExists_addsAndCreatesMutation() {
        var account = new WalletAccount(UUID.randomUUID(), storeId, new BigDecimal("100000.0000"), 0L);
        when(walletMutationPort.findByReferenceIdAndReferenceType(referenceId, MutationReferenceType.TOPUP))
                .thenReturn(Optional.empty());
        when(walletAccountPort.findByStoreIdWithLock(storeId)).thenReturn(Optional.of(account));
        when(walletAccountPort.save(any())).thenAnswer(i -> i.getArgument(0));
        when(walletMutationPort.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.credit(storeId, new BigDecimal("50000.0000"), referenceId,
                MutationReferenceType.TOPUP, "Test topup");

        assertThat(result.newBalance()).isEqualByComparingTo(new BigDecimal("150000.0000"));

        ArgumentCaptor<WalletMutation> captor = ArgumentCaptor.forClass(WalletMutation.class);
        verify(walletMutationPort).save(captor.capture());
        assertThat(captor.getValue().mutationType()).isEqualTo(MutationType.CREDIT);
    }

    @Test
    void credit_whenAccountDoesNotExist_createsAccountThenCredits() {
        var newAccount = new WalletAccount(UUID.randomUUID(), storeId, BigDecimal.ZERO, 0L);
        when(walletMutationPort.findByReferenceIdAndReferenceType(any(), any())).thenReturn(Optional.empty());
        when(walletAccountPort.findByStoreIdWithLock(storeId)).thenReturn(Optional.empty());
        when(walletAccountPort.save(any())).thenReturn(newAccount);
        when(walletMutationPort.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.credit(storeId, new BigDecimal("50000"), referenceId,
                MutationReferenceType.TOPUP, "First topup");

        assertThat(result.newBalance()).isEqualByComparingTo(new BigDecimal("50000"));
        verify(walletAccountPort, times(2)).save(any()); // once to create, once to update balance
    }

    @Test
    void credit_whenDuplicate_returnsExistingResult() {
        UUID existingMutationId = UUID.randomUUID();
        var existingMutation = new WalletMutation(existingMutationId, storeId, new BigDecimal("50000"),
                MutationType.CREDIT, new BigDecimal("150000"), MutationReferenceType.TOPUP,
                referenceId, "existing", null);
        when(walletMutationPort.findByReferenceIdAndReferenceType(referenceId, MutationReferenceType.TOPUP))
                .thenReturn(Optional.of(existingMutation));

        var result = service.credit(storeId, new BigDecimal("50000"), referenceId,
                MutationReferenceType.TOPUP, "duplicate");

        assertThat(result.mutationId()).isEqualTo(existingMutationId);
        verify(walletAccountPort, never()).findByStoreIdWithLock(any());
    }

    // --- refund ---

    @Test
    void refund_whenValid_restoresBalance() {
        var account = new WalletAccount(UUID.randomUUID(), storeId, new BigDecimal("50000.0000"), 0L);
        when(walletMutationPort.findByReferenceIdAndReferenceType(referenceId, MutationReferenceType.REFUND))
                .thenReturn(Optional.empty());
        when(walletAccountPort.findByStoreIdWithLock(storeId)).thenReturn(Optional.of(account));
        when(walletAccountPort.save(any())).thenAnswer(i -> i.getArgument(0));
        when(walletMutationPort.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.refund(storeId, new BigDecimal("10000.0000"), referenceId, "Test refund");

        assertThat(result.newBalance()).isEqualByComparingTo(new BigDecimal("60000.0000"));

        ArgumentCaptor<WalletMutation> captor = ArgumentCaptor.forClass(WalletMutation.class);
        verify(walletMutationPort).save(captor.capture());
        assertThat(captor.getValue().mutationType()).isEqualTo(MutationType.REFUND);
    }

    @Test
    void refund_whenDuplicate_returnsExistingResult() {
        UUID existingMutationId = UUID.randomUUID();
        var existingMutation = new WalletMutation(existingMutationId, storeId, new BigDecimal("10000"),
                MutationType.REFUND, new BigDecimal("60000"), MutationReferenceType.REFUND,
                referenceId, "existing", null);
        when(walletMutationPort.findByReferenceIdAndReferenceType(referenceId, MutationReferenceType.REFUND))
                .thenReturn(Optional.of(existingMutation));

        var result = service.refund(storeId, new BigDecimal("10000"), referenceId, "duplicate");

        assertThat(result.mutationId()).isEqualTo(existingMutationId);
        verify(walletAccountPort, never()).findByStoreIdWithLock(any());
    }

    @Test
    void refund_whenAccountNotFound_throwsException() {
        when(walletMutationPort.findByReferenceIdAndReferenceType(any(), any())).thenReturn(Optional.empty());
        when(walletAccountPort.findByStoreIdWithLock(storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refund(storeId, new BigDecimal("100"), referenceId, "test"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- getMutations ---

    @Test
    void getMutations_returnsListFromPort() {
        var mutation = new WalletMutation(UUID.randomUUID(), storeId, new BigDecimal("10000"),
                MutationType.DEBIT, new BigDecimal("90000"), MutationReferenceType.TRANSACTION,
                UUID.randomUUID(), "test", null);
        when(walletMutationPort.findByStoreIdOrderByCreatedAtDesc(storeId)).thenReturn(List.of(mutation));

        var result = service.getMutations(storeId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().mutationType()).isEqualTo(MutationType.DEBIT);
    }
}
