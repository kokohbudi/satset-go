package com.omnip.catalog.domain.port.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProductRequest(
    @NotNull UUID categoryId,
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 100) String providerName,
    String description,
    String iconUrl,
    boolean active,
    int sortOrder
) {}
