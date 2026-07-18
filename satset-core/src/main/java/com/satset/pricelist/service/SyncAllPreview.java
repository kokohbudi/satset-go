package com.satset.pricelist.service;

import java.util.List;

/** Read-only summary of what a full syncAll() would change. Labels only; no selection keys. */
public record SyncAllPreview(
        List<String> newCategories,
        List<String> newProducts,
        List<String> newDenoms,
        List<String> priceChanges,
        List<String> removed) {}
