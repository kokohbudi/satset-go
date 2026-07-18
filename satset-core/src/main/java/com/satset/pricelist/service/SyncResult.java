package com.satset.pricelist.service;
/** Ringkasan hasil apply sync katalog. */
public record SyncResult(int added, int updated, int deleted, int skipped, int failed) {}
