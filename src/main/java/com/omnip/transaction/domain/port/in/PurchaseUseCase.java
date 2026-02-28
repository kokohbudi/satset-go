package com.omnip.transaction.domain.port.in;

import com.omnip.transaction.domain.model.TransactionSummary;
import com.omnip.shared.exception.InsufficientBalanceException;

import java.util.UUID;

public interface PurchaseUseCase {

    TransactionSummary createPurchase(UUID storeId, UUID denomId, String targetNumber)
            throws InsufficientBalanceException;
}
