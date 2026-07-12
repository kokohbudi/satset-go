package com.satset.shared.exception;

/**
 * Kegagalan dari sistem supplier eksternal (mis. Digiflazz) yang pesan + kode-nya
 * layak ditampilkan ke admin (status supplier, bukan detail internal).
 * {@code code} = kode error supplier apa adanya (mis. rc Digiflazz "83").
 */
public class SupplierException extends RuntimeException {
    private final String code;

    public SupplierException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
