package com.satset.supplier.model;
/** Satu item preview sync. key = identitas buat apply (nama/brand/sku utk ADD, UUID utk DELETE, sku denom utk UPDATE). */
public record SyncPreviewItem(SyncAction action, String key, String label, String detail) {}
