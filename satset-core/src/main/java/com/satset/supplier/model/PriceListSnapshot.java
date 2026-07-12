package com.satset.supplier.model;

import java.time.LocalDateTime;
import java.util.List;

/** Cached DF pricelist plus the moment the cache entry was filled. */
public record PriceListSnapshot(List<PriceListItem> items, LocalDateTime fetchedAt) {}
