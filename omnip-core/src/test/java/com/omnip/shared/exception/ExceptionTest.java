package com.omnip.shared.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    // ==================== BusinessException ====================

    @Test
    void businessException_NoArgs_HasDefaultCode() {
        BusinessException ex = new BusinessException();

        assertEquals("UNKNOWN_ERROR", ex.getErrorCode());
        assertNotNull(ex.getErrorMessage());
    }

    @Test
    void businessException_WithMessage_SetsCode() {
        BusinessException ex = new BusinessException("Email sudah terdaftar");

        assertEquals("UNKNOWN_ERROR", ex.getErrorCode());
        assertEquals("Email sudah terdaftar", ex.getErrorMessage());
        assertEquals("Email sudah terdaftar", ex.getMessage());
    }

    @Test
    void businessException_WithCodeAndMessage_SetsBoth() {
        BusinessException ex = new BusinessException("EMAIL_TAKEN", "Email sudah dipakai");

        assertEquals("EMAIL_TAKEN", ex.getErrorCode());
        assertEquals("Email sudah dipakai", ex.getErrorMessage());
    }

    @Test
    void businessException_WithCause_WrapsIt() {
        RuntimeException cause = new RuntimeException("original");
        BusinessException ex = new BusinessException("ERR", "msg", cause);

        assertSame(cause, ex.getCause());
        assertEquals("ERR", ex.getErrorCode());
    }

    @Test
    void businessException_ToString_ContainsCodeAndMessage() {
        BusinessException ex = new BusinessException("MY_CODE", "my message");

        String s = ex.toString();
        assertTrue(s.contains("MY_CODE"));
        assertTrue(s.contains("my message"));
    }

    // ==================== ResourceNotFoundException ====================

    @Test
    void resourceNotFoundException_MessageContainsNameAndId() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Store", "abc-123");

        assertEquals("abc-123", ex.getResourceId());
        assertEquals("Store", ex.getResourceName());
        assertTrue(ex.getMessage().contains("Store"));
        assertTrue(ex.getMessage().contains("abc-123"));
    }

    @Test
    void resourceNotFoundException_IsRuntimeException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", 42);

        assertInstanceOf(RuntimeException.class, ex);
    }

    // ==================== InsufficientBalanceException ====================

    @Test
    void insufficientBalanceException_SetsCodeToInsufficientBalance() {
        InsufficientBalanceException ex = new InsufficientBalanceException("Saldo tidak cukup");

        assertEquals("INSUFFICIENT_BALANCE", ex.getErrorCode());
        assertEquals("Saldo tidak cukup", ex.getErrorMessage());
    }

    @Test
    void insufficientBalanceException_IsBusinessException() {
        InsufficientBalanceException ex = new InsufficientBalanceException("msg");

        assertInstanceOf(BusinessException.class, ex);
    }
}
