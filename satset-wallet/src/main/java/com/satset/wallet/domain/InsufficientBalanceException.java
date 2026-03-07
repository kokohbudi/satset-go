package com.satset.wallet.domain;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient balance: requested %s, available %s", requested, available));
    }
}