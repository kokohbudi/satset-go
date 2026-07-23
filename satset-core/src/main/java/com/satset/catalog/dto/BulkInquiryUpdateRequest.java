package com.satset.catalog.dto;

import java.util.UUID;

/** Satu item bulk update flag requiresInquiry (inline edit). */
public record BulkInquiryUpdateRequest(UUID id, boolean requiresInquiry) {
}
