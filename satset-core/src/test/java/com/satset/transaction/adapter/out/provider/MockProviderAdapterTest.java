package com.satset.transaction.adapter.out.provider;

import com.satset.transaction.domain.model.ProviderResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MockProviderAdapterTest {

    private final MockProviderAdapter adapter = new MockProviderAdapter();

    @Test
    void sendTransaction_ReturnsNonNullResponse() {
        ProviderResponse response = adapter.sendTransaction("08123456789", "TEL10K", new BigDecimal("10000"));

        assertNotNull(response);
    }

    @Test
    void sendTransaction_Success_HasReferenceAndSN() {
        // Run multiple times to hit success path (90% probability)
        // With 100 tries, probability of never succeeding is 0.1^100 ≈ 0
        ProviderResponse successResponse = null;
        for (int i = 0; i < 100; i++) {
            ProviderResponse r = adapter.sendTransaction("08123456789", "TEL10K", new BigDecimal("10000"));
            if (r.success()) {
                successResponse = r;
                break;
            }
        }

        assertNotNull(successResponse, "Expected at least one success in 100 tries");
        assertNotNull(successResponse.referenceNumber());
        assertTrue(successResponse.referenceNumber().startsWith("REF-"));
        assertNotNull(successResponse.serialNumber());
        assertNotNull(successResponse.message());
    }

    @Test
    void sendTransaction_Failure_HasNullRefAndSN() {
        // Run multiple times to hit failure path (10% probability)
        // With 100 tries, probability of never failing is 0.9^100 ≈ 2.6e-5
        ProviderResponse failResponse = null;
        for (int i = 0; i < 100; i++) {
            ProviderResponse r = adapter.sendTransaction("08123456789", "TEL10K", new BigDecimal("10000"));
            if (!r.success()) {
                failResponse = r;
                break;
            }
        }

        assertNotNull(failResponse, "Expected at least one failure in 100 tries");
        assertNull(failResponse.referenceNumber());
        assertNull(failResponse.serialNumber());
        assertNotNull(failResponse.message());
    }
}
