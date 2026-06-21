package com.satset.shared.exception;

/**
 * Exception kustom untuk kesalahan bisnis dalam aplikasi.
 * Kelas ini menyediakan cara terstruktur untuk menangani kesalahan bisnis
 * dengan kode error dan pesan yang spesifik.
 */
public class BusinessException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String errorMessage;

    /**
     * Konstruktor dengan pesan error.
     * Membuat exception dengan kode error "UNKNOWN_ERROR" dan pesan yang ditentukan.
     *
     * @param errorMessage Pesan error yang akan ditampilkan
     */
    public BusinessException(String errorMessage) {
        super(errorMessage);
        this.errorCode = "UNKNOWN_ERROR";
        this.errorMessage = errorMessage;
    }

    /**
     * Konstruktor dengan kode error dan pesan.
     * Membuat exception dengan kode dan pesan error yang ditentukan.
     *
     * @param errorCode    Kode error kustom
     * @param errorMessage Pesan error yang akan ditampilkan
     */
    public BusinessException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * Mendapatkan kode error.
     *
     * @return String berisi kode error
     */
    public String getErrorCode() {
        return this.errorCode;
    }

    /**
     * Mendapatkan pesan error.
     *
     * @return String berisi pesan error
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * Representasi string dari exception ini.
     *
     * @return String yang berisi representasi terformat dari exception
     */
    @Override
    public String toString() {
        return "BusinessException [errorCode=" + this.errorCode + ", errorMessage=" + this.errorMessage + "]";
    }
}