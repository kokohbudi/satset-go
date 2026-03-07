package com.omnip.catalog.domain.port.in;

import com.omnip.catalog.domain.model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 100) String name,
    @NotNull CategoryType categoryType,
    String iconUrl,
    boolean active,
    int sortOrder
) {}
