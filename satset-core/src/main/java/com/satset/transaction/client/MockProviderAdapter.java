package com.satset.transaction.client;

import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.ProviderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "supplier.mode", havingValue = "mock", matchIfMissing = true)
public class MockProviderAdapter implements ProviderPort {

    @Override
    public ProviderResponse sendTransaction(String targetNumber, String denomCode,
                                            BigDecimal amount, String refId) {
        log.info("Mock provider: sending {} {} to {} (ref={})", denomCode, amount, targetNumber, refId);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean success = Math.random() < 0.9;

        if (success) {
            String ref = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String sn = "SN" + System.currentTimeMillis();
            log.info("Mock provider: SUCCESS ref={} sn={}", ref, sn);
            return new ProviderResponse(ProviderStatus.SUCCESS, ref, sn, "Transaksi berhasil", null);
        }

        log.warn("Mock provider: FAILED for target={}", targetNumber);
        return new ProviderResponse(ProviderStatus.FAILED, null, null, "Transaksi gagal dari provider", null);
    }
}
