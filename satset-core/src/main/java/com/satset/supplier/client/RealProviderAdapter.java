package com.satset.supplier.client;

import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.model.ProviderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * Adapter supplier ASLI — nembak keluar lewat {@code providerRestClient} biar
 * egress = IP statis Fly ({@code 209.71.95.98}) yang di-whitelist supplier.
 *
 * <p>Aktif kalau {@code supplier.mode=real}. Default {@link MockProviderAdapter}.
 *
 * <p>ponytail: STUB. Endpoint/format request/parse response supplier belum ada
 * kontraknya — jangan tebak. Isi {@link #sendTransaction} pas dok supplier turun;
 * wiring proxy + switch Mock↔Real udah siap.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "supplier.mode", havingValue = "real")
public class RealProviderAdapter implements ProviderPort {

    private final RestClient http; // = providerRestClient (lewat proxy Fly)

    public RealProviderAdapter(RestClient providerRestClient) {
        this.http = providerRestClient;
    }

    @Override
    public ProviderResponse sendTransaction(String targetNumber, String denomCode,
                                            BigDecimal amount, String refId) {
        throw new UnsupportedOperationException(
                "Kontrak API supplier belum ada — pakai supplier.mode=mock sampai dok turun");
    }
}
