package com.satset.supplier.service.pricelist;
/** Ringkasan hasil apply sync katalog. */
public record SyncResult(int added, int updated, int deleted, int skipped, int failed) {}
