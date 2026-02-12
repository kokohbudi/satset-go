package com.omnip.dtos;

import com.omnip.enums.CategoryType;
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
}