package com.omnip.transaction.domain.port.in;

import com.omnip.transaction.domain.model.Transactions;
import com.omnip.shared.exception.InsufficientBalanceException;

import java.util.UUID;

public interface PurchaseUseCase {

    Transactions createPurchase(UUID storeId, UUID denomId, String targetNumber)
            throws InsufficientBalanceException;
}
