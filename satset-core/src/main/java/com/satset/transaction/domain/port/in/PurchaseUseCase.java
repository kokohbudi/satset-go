package com.satset.transaction.domain.port.in;

import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.transaction.domain.model.TransactionSummary;

import java.util.UUID;

public interface PurchaseUseCase {

    TransactionSummary createPurchase(UUID storeId, String walletId, UUID denomId, String targetNumber)
            throws InsufficientBalanceException;
}
