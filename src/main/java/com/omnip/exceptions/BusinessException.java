package com.omnip.exceptions;

public class BusinessException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String errorMessage;

    // Konstruktor tanpa argumen
    public BusinessException() {
        super();
        this.errorCode = "UNKNOWN_ERROR";
        this.errorMessage = "An unknown business error occurred.";
    }

    // Konstruktor dengan pesan error
    public BusinessException(String errorMessage) {
        super(errorMessage);
        this.errorCode = "UNKNOWN_ERROR";
        this.errorMessage = errorMessage;
    }

    // Konstruktor dengan kode error dan pesan
    public BusinessException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    // Konstruktor dengan kode error, pesan, dan Throwable
    public BusinessException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    // Getter untuk kode error
    public String getErrorCode() {
        return this.errorCode;
    }

    // Getter untuk pesan error
    public String getErrorMessage() {
        return this.errorMessage;
    }

    // Optional: Override metode toString() untuk memberikan representasi yang lebih informatif
    @Override
    public String toString() {
        return "BusinessException [errorCode=" + this.errorCode + ", errorMessage=" + this.errorMessage + "]";
    }
}
