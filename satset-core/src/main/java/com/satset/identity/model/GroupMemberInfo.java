package com.satset.identity.model;

public record GroupMemberInfo(
        String providerUserId,
        String username,
        String fullname,
        String email,
        boolean active) {
}
