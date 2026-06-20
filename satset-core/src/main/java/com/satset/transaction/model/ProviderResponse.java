package com.satset.transaction.model;

public record ProviderResponse(
        boolean success,
        String referenceNumber,
        String serialNumber,
        String message) {
}
