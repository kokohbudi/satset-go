package com.omnip.identity.domain.model;

public record GroupMemberInfo(
        String providerUserId,
        String username,
        String fullname,
        String email,
        boolean active) {
}
