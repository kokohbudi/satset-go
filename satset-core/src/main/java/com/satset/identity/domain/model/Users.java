package com.satset.identity.domain.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class Users {
    private UUID id;
    private String email;
    private String username;
    private String fullname;
    private String registrationChannel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String createdBy;
    private boolean active;
    private boolean deleted;
    private String providerUserId;
    private Long version;
    private UUID storeId;
    private List<String> roles;
}