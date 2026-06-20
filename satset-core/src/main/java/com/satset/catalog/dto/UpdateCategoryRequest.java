package com.satset.catalog.dto;

import com.satset.catalog.model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 100) String name,
    @NotNull CategoryType categoryType,
    String iconUrl,
    boolean active,
    int sortOrder
) {}
