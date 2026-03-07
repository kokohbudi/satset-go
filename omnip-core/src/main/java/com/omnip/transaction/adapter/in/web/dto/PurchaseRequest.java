package com.omnip.transaction.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PurchaseRequest(
                @NotNull UUID denomId,
                @NotBlank String targetNumber) {
}
