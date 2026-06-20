package com.satset.transaction.client;

import com.satset.transaction.model.ProviderResponse;

import java.math.BigDecimal;

public interface ProviderPort {

    ProviderResponse sendTransaction(String targetNumber, String denomCode, BigDecimal amount);
}
