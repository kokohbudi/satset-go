package com.satset.transaction.client;

import com.satset.transaction.model.InquiryResult;
import com.satset.transaction.model.ProviderResponse;

import java.math.BigDecimal;

public interface ProviderPort {

    ProviderResponse sendTransaction(String targetNumber, String denomCode,
                                     BigDecimal amount, String refId);

    InquiryResult inquiry(String customerNo, String denomCode, String refId, BigDecimal amount);

    ProviderResponse payPostpaid(String customerNo, String denomCode, String refId);
}
