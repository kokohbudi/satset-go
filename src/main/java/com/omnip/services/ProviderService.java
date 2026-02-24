package com.omnip.services;

import com.omnip.dtos.ProviderResponse;

import java.math.BigDecimal;

public interface ProviderService {

    ProviderResponse sendTransaction(String targetNumber, String denomCode, BigDecimal amount);
}
