package com.omnip.transaction.domain.port.in;

import com.omnip.transaction.domain.model.Transactions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TransactionQueryUseCase {

    Transactions getTransaction(UUID id, UUID storeId);

    Page<Transactions> getTransactionHistory(UUID storeId, Pageable pageable);
}
