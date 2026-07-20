package com.satset.transaction.client;

import com.satset.transaction.model.ProviderStatus;

import java.util.Set;

/**
 * Pemetaan status/rc Digiflazz -> {@link ProviderStatus}, dipakai bareng oleh
 * {@link RealProviderAdapter} (sync topup) dan webhook inbound (satset-webhook)
 * biar tabelnya satu, gak dobel.
 *
 * <p>Money-safe: status tak dikenal / Gagal dgn rc yang bisa membentuk transaksi
 * (timeout, not-found) dipetakan ke PENDING agar tidak auto-refund.
 */
public final class DigiflazzStatusMapper {

    // rc "Gagal" yang berarti transaksi benar-benar tidak terbentuk -> boleh refund.
    // rc timeout(01)/not-found(50) BISA membentuk transaksi -> jangan refund, biar poll/webhook yang settle.
    private static final Set<String> FORMS_TRANSACTION = Set.of("01", "50");

    private DigiflazzStatusMapper() {
    }

    public static ProviderStatus map(String dfStatus, String rc) {
        if ("Sukses".equalsIgnoreCase(dfStatus)) return ProviderStatus.SUCCESS;
        if ("Gagal".equalsIgnoreCase(dfStatus) && rc != null && !FORMS_TRANSACTION.contains(rc)) return ProviderStatus.FAILED;
        // Pending, Gagal+forms-transaction, null/unknown -> PENDING (poll/webhook resolves)
        return ProviderStatus.PENDING;
    }
}
