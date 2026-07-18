package com.satset.transaction.model;

import java.math.BigDecimal;

public record ProviderResponse(
        ProviderStatus status,
        String referenceNumber,
        String serialNumber,
        String message,
        BigDecimal cost) {

    /** True only for a settled-successful transaction. */
    public boolean success() {
        return status == ProviderStatus.SUCCESS;
    }
}
