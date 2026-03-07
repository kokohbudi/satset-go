package com.satset.transaction.domain.model;

public record ProviderResponse(
        boolean success,
        String referenceNumber,
        String serialNumber,
        String message) {
}
