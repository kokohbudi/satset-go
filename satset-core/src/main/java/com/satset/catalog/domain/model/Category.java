package com.satset.catalog.domain.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Category {
    private UUID id;
    private String code;
    private String name;
    private CategoryType categoryType;
    private String iconUrl;
    private boolean active;
    private boolean deleted;
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;
}