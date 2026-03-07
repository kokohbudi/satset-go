package com.satset.catalog.domain.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProductDenomMeta {
    private UUID id;
    private UUID productDenomId;
    private String metaKey;
    private String metaValue;
    private LocalDateTime createdAt;
}