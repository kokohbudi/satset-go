package com.omnip.onboarding.domain.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Stores {
    private UUID id;
    private String name;
    private String referralId;
    private String email;
    private String phone;

    /**
     * @deprecated Since WR-8. Balance sekarang disimpan di WalletAccount entity.
     *             Field ini hanya untuk backward compatibility dan akan dihapus di versi mendatang.
     *             Source of truth: transaction.domain.model.WalletAccount.balance
     * @see com.omnip.transaction.domain.model.WalletAccount
     */
    @Deprecated(since = "WR-8", forRemoval = true)
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * FK to Keycloak Organization ID — used for multi-tenancy organization
     * isolation.
     */
    private String keycloakOrganizationId;

    private boolean active;
    private boolean deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    /**
     * Reference to upline store (self-referencing relationship).
     * Uses UUID instead of object reference to avoid JPA coupling.
     */
    private UUID uplineId;

    private Long version;
}