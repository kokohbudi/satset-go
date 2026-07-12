package com.satset.supplier.service;

import java.time.LocalDateTime;
import java.util.Map;

/** Global SKU(upper) -> DF cost map plus the cache fill time, for the admin Harga Suplier column. */
public record SupplierPriceView(LocalDateTime fetchedAt, Map<String, Long> prices) {}
