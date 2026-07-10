package com.satset.catalog.service;
/** Normalisasi nama display supplier → code katalog: UPPERCASE, buang non-alfanumerik. */
public final class CatalogCodeUtil {
    private CatalogCodeUtil() {}
    public static String toCode(String name) {
        return name == null ? "" : name.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
