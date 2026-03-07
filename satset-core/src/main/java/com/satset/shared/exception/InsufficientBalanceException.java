package com.satset.shared.exception;

public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(String message) {
        super("INSUFFICIENT_BALANCE", message);
    }
}
