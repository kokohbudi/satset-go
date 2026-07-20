package com.satset.transaction.client;

import com.satset.digiflazz.client.DigiflazzClient;
import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.ProviderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Adapter supplier ASLI — delegasi ke {@link DigiflazzClient#topup} lalu memetakan
 * status/rc Digiflazz ke {@link ProviderStatus} lewat {@link DigiflazzStatusMapper}.
 *
 * <p>Satu-satunya {@link ProviderPort} — real vs sandbox diatur di sisi Digiflazz
 * (mode akun development/production), bukan lewat flag aplikasi.
 */
@Slf4j
@Service
public class RealProviderAdapter implements ProviderPort {

    private final DigiflazzClient digiflazz;

    public RealProviderAdapter(DigiflazzClient digiflazz) {
        this.digiflazz = digiflazz;
    }

    @Override
    public ProviderResponse sendTransaction(String targetNumber, String denomCode,
                                            BigDecimal amount, String refId) {
        var r = digiflazz.topup(refId, denomCode, targetNumber);
        ProviderStatus status = DigiflazzStatusMapper.map(r.status(), r.rc());
        log.info("Digiflazz /transaction refId={} status={} rc={} -> {}", refId, r.status(), r.rc(), status);
        return new ProviderResponse(status, r.refId(), emptyToNull(r.sn()), r.message(), r.price());
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
