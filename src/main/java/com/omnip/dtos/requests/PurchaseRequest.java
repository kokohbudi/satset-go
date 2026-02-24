package com.omnip.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PurchaseRequest(
                @NotNull UUID denomId,
                @NotBlank String targetNumber) {
}
