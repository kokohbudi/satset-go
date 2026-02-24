package com.omnip.catalog.adapter.in.web.dto;

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
    private String categoryCode;
    private String categoryName;
}