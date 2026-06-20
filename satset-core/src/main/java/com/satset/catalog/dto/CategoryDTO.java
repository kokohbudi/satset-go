package com.satset.catalog.dto;

import com.satset.catalog.model.CategoryType;
import lombok.Data;

import java.util.UUID;

@Data
public class CategoryDTO {
    private UUID id;
    private String code;
    private String name;
    private CategoryType categoryType;
    private String iconUrl;
    private int sortOrder;
    private boolean active;
    private boolean deleted;
}