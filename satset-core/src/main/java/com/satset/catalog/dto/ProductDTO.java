package com.satset.catalog.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ProductDTO {
    private UUID id;
    private String code;
    private String name;
    private String providerName;
    private String description;
    private String iconUrl;
    private UUID categoryId;
    private int sortOrder;
    private boolean active;
    private boolean deleted;
    private boolean inSupplier;
}