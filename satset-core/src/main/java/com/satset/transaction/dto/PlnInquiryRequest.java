package com.satset.transaction.dto;

import jakarta.validation.constraints.NotBlank;

public record PlnInquiryRequest(@NotBlank String customerNo) {
}
