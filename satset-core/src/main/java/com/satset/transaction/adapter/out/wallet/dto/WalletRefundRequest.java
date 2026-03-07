package com.satset.transaction.adapter.out.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletRefundRequest(
    UUID storeId,
    BigDecimal amount,
    UUID originalReferenceId,
    String description
) {}
