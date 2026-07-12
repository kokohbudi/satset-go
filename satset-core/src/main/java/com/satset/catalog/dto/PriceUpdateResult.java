package com.satset.catalog.dto;

import java.util.UUID;

/** Hasil per-item bulk update harga. {@code error} null kalau ok. */
public record PriceUpdateResult(UUID id, String code, boolean ok, String error) {

    public static PriceUpdateResult ok(UUID id, String code) {
        return new PriceUpdateResult(id, code, true, null);
    }

    public static PriceUpdateResult fail(UUID id, String code, String error) {
        return new PriceUpdateResult(id, code, false, error);
    }
}
