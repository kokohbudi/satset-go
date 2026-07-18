package com.satset.supplier.client;

import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.ProviderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Adapter supplier ASLI — delegasi ke {@link DigiflazzClient#topup} lalu memetakan
 * status/rc Digiflazz ke {@link ProviderStatus}.
 *
 * <p>Money-safe: status tak dikenal / Gagal dgn rc yang bisa membentuk transaksi
 * (timeout, not-found) dipetakan ke PENDING agar tidak auto-refund (poll yang menyelesaikan).
 *
 * <p>Satu-satunya {@link ProviderPort} — real vs sandbox diatur di sisi Digiflazz
 * (mode akun development/production), bukan lewat flag aplikasi.
 */
@Slf4j
@Service
public class RealProviderAdapter implements ProviderPort {

    // rc "Gagal" yang berarti transaksi benar-benar tidak terbentuk -> boleh refund.
    // rc timeout(01)/not-found(50) BISA membentuk transaksi -> jangan refund, biar poll yang settle.
    private static final Set<String> FORMS_TRANSACTION = Set.of("01", "50");

    private final DigiflazzClient digiflazz;

    public RealProviderAdapter(DigiflazzClient digiflazz) {
        this.digiflazz = digiflazz;
    }

    @Override
    public ProviderResponse sendTransaction(String targetNumber, String denomCode,
                                            BigDecimal amount, String refId) {
        var r = digiflazz.topup(refId, denomCode, targetNumber);
        ProviderStatus status = mapStatus(r.status(), r.rc());
        log.info("Digiflazz /transaction refId={} status={} rc={} -> {}", refId, r.status(), r.rc(), status);
        return new ProviderResponse(status, r.refId(), emptyToNull(r.sn()), r.message(), r.price());
    }

    private static ProviderStatus mapStatus(String dfStatus, String rc) {
        if ("Sukses".equalsIgnoreCase(dfStatus)) return ProviderStatus.SUCCESS;
        if ("Gagal".equalsIgnoreCase(dfStatus) && rc != null && !FORMS_TRANSACTION.contains(rc)) return ProviderStatus.FAILED;
        // Pending, Gagal+forms-transaction, null/unknown -> PENDING (poll resolves)
        return ProviderStatus.PENDING;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
