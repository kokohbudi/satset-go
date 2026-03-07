package com.omnip.transaction.domain.port.in;

import com.omnip.transaction.domain.model.MutationResult;
import com.omnip.transaction.domain.model.MutationReferenceType;
import com.omnip.shared.exception.InsufficientBalanceException;

import java.math.BigDecimal;
import java.util.UUID;

public interface BalanceManagementUseCase {

    MutationResult deductBalance(UUID storeId, BigDecimal amount,
            MutationReferenceType referenceType, UUID referenceId, String description)
            throws InsufficientBalanceException;

    MutationResult addBalance(UUID storeId, BigDecimal amount,
            MutationReferenceType referenceType, UUID referenceId, String description);

    BigDecimal getBalance(UUID storeId);
}
