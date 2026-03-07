package com.omnip.catalog.adapter.in.web.dto;

import com.omnip.catalog.domain.model.CategoryType;
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