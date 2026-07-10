package com.satset.transaction.model;

import java.math.BigDecimal;

public record ProviderResponse(
        boolean success,
        String referenceNumber,
        String serialNumber,
        String message,
        BigDecimal cost) {
}
