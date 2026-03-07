package com.satset.transaction.domain.port.in;

import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.transaction.domain.model.TransactionSummary;

import java.util.UUID;

public interface PurchaseUseCase {

    TransactionSummary createPurchase(UUID storeId, UUID denomId, String targetNumber)
            throws InsufficientBalanceException;
}
