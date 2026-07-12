package com.satset.catalog.dto;

import java.util.UUID;

/** Satu item bulk update nama produk (inline edit). */
public record BulkNameUpdateRequest(UUID id, String name) {
}
