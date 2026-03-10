package com.satset.transaction.domain.port.in;

import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.transaction.domain.model.MutationReferenceType;
import com.satset.transaction.domain.model.MutationResult;

import java.math.BigDecimal;
import java.util.UUID;

public interface BalanceManagementUseCase {

    MutationResult deductBalance(String walletId, BigDecimal amount,
            MutationReferenceType referenceType, UUID referenceId, String description)
            throws InsufficientBalanceException;

    MutationResult addBalance(String walletId, BigDecimal amount,
            MutationReferenceType referenceType, UUID referenceId, String description);

    BigDecimal getBalance(String walletId);
}
