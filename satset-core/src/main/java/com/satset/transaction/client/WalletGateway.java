package com.satset.transaction.client;

import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.wallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Boundary between the transaction module and the wallet module: maps enums + exceptions
 * and hides wallet-internal types. In-process now; swap for a remote impl behind config when split.
 */
@Component
public class WalletGateway {

    private static final Logger log = LoggerFactory.getLogger(WalletGateway.class);

    private final WalletService walletService;

    public WalletGateway(WalletService walletService) {
        this.walletService = walletService;
    }

    public void deductBalance(String walletId, BigDecimal amount,
            UUID referenceId, String description)
            throws InsufficientBalanceException {

        try {
            var result = walletService.debit(walletId, amount, referenceId,
                    com.satset.wallet.model.MutationReferenceType.TRANSACTION, description);
            log.info("DEBIT wallet={} amount={} balanceAfter={}", walletId, amount, result.newBalance());
        } catch (com.satset.wallet.service.InsufficientBalanceException e) {
            throw new InsufficientBalanceException("Saldo tidak mencukupi");
        } catch (com.satset.wallet.service.ResourceNotFoundException e) {
            throw new ResourceNotFoundException("WalletAccount", walletId);
        }
    }

    public void refundBalance(String walletId, BigDecimal amount,
            UUID referenceId, String description) {

        try {
            var result = walletService.refund(walletId, amount, referenceId, description);
            log.info("REFUND wallet={} amount={} balanceAfter={}", walletId, amount, result.newBalance());
        } catch (com.satset.wallet.service.ResourceNotFoundException e) {
            throw new ResourceNotFoundException("WalletAccount", walletId);
        }
    }

    public BigDecimal getBalance(String walletId) {
        try {
            return walletService.getBalance(walletId);
        } catch (com.satset.wallet.service.ResourceNotFoundException e) {
            throw new ResourceNotFoundException("WalletAccount", walletId);
        }
    }

    public java.util.List<com.satset.wallet.dto.WalletMutationDTO> listMutations(String walletId) {
        return walletService.getMutations(walletId).stream()
                .map(com.satset.wallet.dto.WalletMutationDTO::from)
                .toList();
    }
}
