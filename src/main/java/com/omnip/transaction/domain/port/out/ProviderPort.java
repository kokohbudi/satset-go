package com.omnip.transaction.domain.port.out;

import com.omnip.transaction.domain.model.ProviderResponse;

import java.math.BigDecimal;

public interface ProviderPort {

    ProviderResponse sendTransaction(String targetNumber, String denomCode, BigDecimal amount);
}
