package com.omnip.dtos.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TopUpRequest(
                @NotNull @DecimalMin("1") BigDecimal amount,
                String description) {
}
