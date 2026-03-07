package com.omnip.transaction.domain.port.in;

import com.omnip.transaction.domain.model.TransactionSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TransactionQueryUseCase {

    TransactionSummary getTransaction(UUID id, UUID storeId);

    Page<TransactionSummary> getTransactionHistory(UUID storeId, Pageable pageable);
}
