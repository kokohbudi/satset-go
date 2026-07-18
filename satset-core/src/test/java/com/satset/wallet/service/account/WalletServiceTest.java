package com.satset.wallet.service.account;

import com.satset.wallet.service.idgen.WalletIdGenerator;

import com.satset.wallet.model.MutationReferenceType;
import com.satset.wallet.model.MutationType;
import com.satset.wallet.model.WalletAccountEntity;
import com.satset.wallet.model.WalletMutationEntity;
import com.satset.wallet.repository.WalletAccountRepository;
import com.satset.wallet.repository.WalletMutationRepository;
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
class WalletServiceTest {

    @Mock private WalletAccountRepository walletAccountRepository;
    @Mock private WalletMutationRepository walletMutationRepository;
    @Mock private WalletIdGenerator walletIdGenerator;

    private WalletService service;
    private UUID referenceId;
    private String walletId;

    @BeforeEach
    void setUp() {
        service = new WalletService(walletAccountRepository, walletMutationRepository, walletIdGenerator);
        referenceId = UUID.randomUUID();
        walletId = "7000000001";
    }

    private static WalletAccountEntity account(String walletId, BigDecimal balance) {
        WalletAccountEntity entity = new WalletAccountEntity();
        entity.setWalletId(walletId);
        entity.setBalance(balance);
        return entity;
    }

    private static WalletMutationEntity existingMutation(UUID id, BigDecimal balanceAfter) {
        WalletMutationEntity entity = new WalletMutationEntity();
        entity.setId(id);
        entity.setBalanceAfter(balanceAfter);
        return entity;
    }

    // --- createWallet ---

    @Test
    void createWallet_createsNewWallet() {
        when(walletIdGenerator.generate()).thenReturn(walletId);
        when(walletAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.createWallet();

        assertThat(result.getWalletId()).isEqualTo(walletId);
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(walletAccountRepository).save(any(WalletAccountEntity.class));
    }

    // --- getBalance ---

    @Test
    void getBalance_whenAccountExists_returnsBalance() {
        when(walletAccountRepository.findById(walletId))
                .thenReturn(Optional.of(account(walletId, new BigDecimal("100000.0000"))));

        assertThat(service.getBalance(walletId)).isEqualByComparingTo(new BigDecimal("100000.0000"));
    }

    @Test
    void getBalance_whenAccountDoesNotExist_throwsException() {
        when(walletAccountRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBalance(walletId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- debit ---

    @Test
    void debit_whenSufficientBalance_deductsAndCreatesMutation() {
        when(walletMutationRepository.findByReferenceIdAndReferenceType(referenceId, MutationReferenceType.TRANSACTION))
                .thenReturn(Optional.empty());
        when(walletAccountRepository.findByWalletIdWithLock(walletId))
                .thenReturn(Optional.of(account(walletId, new BigDecimal("100000.0000"))));
        when(walletAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(walletMutationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.debit(walletId, new BigDecimal("50000.0000"), referenceId,
                MutationReferenceType.TRANSACTION, "Test purchase");

        assertThat(result.newBalance()).isEqualByComparingTo(new BigDecimal("50000.0000"));

        ArgumentCaptor<WalletMutationEntity> captor = ArgumentCaptor.forClass(WalletMutationEntity.class);
        verify(walletMutationRepository).save(captor.capture());
        assertThat(captor.getValue().getMutationType()).isEqualTo(MutationType.DEBIT);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("50000.0000"));
        assertThat(captor.getValue().getReferenceId()).isEqualTo(referenceId);
    }

    @Test
    void debit_whenInsufficientBalance_throwsException() {
        when(walletMutationRepository.findByReferenceIdAndReferenceType(referenceId, MutationReferenceType.TRANSACTION))
                .thenReturn(Optional.empty());
        when(walletAccountRepository.findByWalletIdWithLock(walletId))
                .thenReturn(Optional.of(account(walletId, new BigDecimal("10000.0000"))));

        assertThatThrownBy(() -> service.debit(walletId, new BigDecimal("50000.0000"), referenceId,
                MutationReferenceType.TRANSACTION, "Test"))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(walletMutationRepository, never()).save(any());
    }

    @Test
    void debit_whenDuplicate_returnsExistingResult() {
        UUID existingMutationId = UUID.randomUUID();
        when(walletMutationRepository.findByReferenceIdAndReferenceType(referenceId, MutationReferenceType.TRANSACTION))
                .thenReturn(Optional.of(existingMutation(existingMutationId, new BigDecimal("50000"))));

        var result = service.debit(walletId, new BigDecimal("50000"), referenceId,
                MutationReferenceType.TRANSACTION, "duplicate");

        assertThat(result.mutationId()).isEqualTo(existingMutationId);
        verify(walletAccountRepository, never()).findByWalletIdWithLock(any());
        verify(walletMutationRepository, never()).save(any());
    }

    @Test
    void debit_whenAccountNotFound_throwsException() {
        when(walletMutationRepository.findByReferenceIdAndReferenceType(any(), any())).thenReturn(Optional.empty());
        when(walletAccountRepository.findByWalletIdWithLock(walletId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.debit(walletId, new BigDecimal("100"), referenceId,
                MutationReferenceType.TRANSACTION, "test"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- credit ---

    @Test
    void credit_whenAccountExists_addsAndCreatesMutation() {
        when(walletMutationRepository.findByReferenceIdAndReferenceType(referenceId, MutationReferenceType.TOPUP))
                .thenReturn(Optional.empty());
        when(walletAccountRepository.findByWalletIdWithLock(walletId))
                .thenReturn(Optional.of(account(walletId, new BigDecimal("100000.0000"))));
        when(walletAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(walletMutationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.credit(walletId, new BigDecimal("50000.0000"), referenceId,
                MutationReferenceType.TOPUP, "Test topup");

        assertThat(result.newBalance()).isEqualByComparingTo(new BigDecimal("150000.0000"));

        ArgumentCaptor<WalletMutationEntity> captor = ArgumentCaptor.forClass(WalletMutationEntity.class);
        verify(walletMutationRepository).save(captor.capture());
        assertThat(captor.getValue().getMutationType()).isEqualTo(MutationType.CREDIT);
    }

    @Test
    void credit_whenAccountNotFound_throwsException() {
        when(walletMutationRepository.findByReferenceIdAndReferenceType(any(), any())).thenReturn(Optional.empty());
        when(walletAccountRepository.findByWalletIdWithLock(walletId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.credit(walletId, new BigDecimal("50000"), referenceId,
                MutationReferenceType.TOPUP, "Test"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void credit_whenDuplicate_returnsExistingResult() {
        UUID existingMutationId = UUID.randomUUID();
        when(walletMutationRepository.findByReferenceIdAndReferenceType(referenceId, MutationReferenceType.TOPUP))
                .thenReturn(Optional.of(existingMutation(existingMutationId, new BigDecimal("150000"))));

        var result = service.credit(walletId, new BigDecimal("50000"), referenceId,
                MutationReferenceType.TOPUP, "duplicate");

        assertThat(result.mutationId()).isEqualTo(existingMutationId);
        verify(walletAccountRepository, never()).findByWalletIdWithLock(any());
    }

    // --- refund ---

    @Test
    void refund_whenValid_restoresBalance() {
        when(walletMutationRepository.findByReferenceIdAndReferenceType(referenceId, MutationReferenceType.REFUND))
                .thenReturn(Optional.empty());
        when(walletAccountRepository.findByWalletIdWithLock(walletId))
                .thenReturn(Optional.of(account(walletId, new BigDecimal("50000.0000"))));
        when(walletAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(walletMutationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.refund(walletId, new BigDecimal("10000.0000"), referenceId, "Test refund");

        assertThat(result.newBalance()).isEqualByComparingTo(new BigDecimal("60000.0000"));

        ArgumentCaptor<WalletMutationEntity> captor = ArgumentCaptor.forClass(WalletMutationEntity.class);
        verify(walletMutationRepository).save(captor.capture());
        assertThat(captor.getValue().getMutationType()).isEqualTo(MutationType.REFUND);
    }

    @Test
    void refund_whenDuplicate_returnsExistingResult() {
        UUID existingMutationId = UUID.randomUUID();
        when(walletMutationRepository.findByReferenceIdAndReferenceType(referenceId, MutationReferenceType.REFUND))
                .thenReturn(Optional.of(existingMutation(existingMutationId, new BigDecimal("60000"))));

        var result = service.refund(walletId, new BigDecimal("10000"), referenceId, "duplicate");

        assertThat(result.mutationId()).isEqualTo(existingMutationId);
        verify(walletAccountRepository, never()).findByWalletIdWithLock(any());
    }

    @Test
    void refund_whenAccountNotFound_throwsException() {
        when(walletMutationRepository.findByReferenceIdAndReferenceType(any(), any())).thenReturn(Optional.empty());
        when(walletAccountRepository.findByWalletIdWithLock(walletId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refund(walletId, new BigDecimal("100"), referenceId, "test"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- getMutations ---

    @Test
    void getMutations_returnsListFromRepository() {
        WalletMutationEntity mutation = new WalletMutationEntity();
        mutation.setMutationType(MutationType.DEBIT);
        when(walletMutationRepository.findByWalletIdOrderByCreatedAtDesc(walletId)).thenReturn(List.of(mutation));

        var result = service.getMutations(walletId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getMutationType()).isEqualTo(MutationType.DEBIT);
    }
}
