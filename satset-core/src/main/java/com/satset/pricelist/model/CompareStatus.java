package com.satset.pricelist.model;

/**
 * Status banding harga beli katalog vs Digiflazz per SKU.
 * NAIK/TURUN dari sudut pandang cost DF relatif cost DB.
 */
public enum CompareStatus {
    SAMA,   // cost DB == cost DF
    NAIK,   // cost DF > cost DB (atau DB belum diset)
    TURUN,  // cost DF < cost DB
    BARU,   // SKU DF belum ada di katalog
    HILANG  // denom katalog gak ada di daftar DF
}
