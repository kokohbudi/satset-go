package com.satset.transaction.adapter.out.provider;

import com.satset.transaction.domain.model.ProviderResponse;
import com.satset.transaction.domain.port.out.ProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class MockProviderAdapter implements ProviderPort {

    @Override
    public ProviderResponse sendTransaction(String targetNumber, String denomCode, BigDecimal amount) {
        log.info("Mock provider: sending {} {} to {}", denomCode, amount, targetNumber);

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
            return new ProviderResponse(true, ref, sn, "Transaksi berhasil");
        }

        log.warn("Mock provider: FAILED for target={}", targetNumber);
        return new ProviderResponse(false, null, null, "Transaksi gagal dari provider");
    }
}
