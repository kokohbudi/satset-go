package com.satset.onboarding.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Store entity. Model + JPA entity merged (layered monolith).
 * Each store has a unique wallet ID for financial transactions.
 */
@Entity
@Table(name = "stores")
@EntityListeners(AuditingEntityListener.class)
@Data
public class Stores {
    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    private String name;
    private String referralId;
    private String email;
    private String phone;

    /**
     * Wallet ID for this store (format: 700xxxxxxx).
     * Nullable for existing stores before this feature was introduced.
     */
    @Column(name = "wallet_id", unique = true, length = 10)
    private String walletId;

    /**
     * FK to Keycloak Organization ID — used for multi-tenancy organization
     * isolation.
     */
    @Column(name = "keycloak_organization_id")
    private String keycloakOrganizationId;

    private boolean active;
    private boolean deleted;

    @CreatedDate
    @Column(name = "created_date")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_date")
    private LocalDateTime updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    /**
     * Reference to upline store (self-referencing).
     * Plain UUID column instead of @ManyToOne — avoids JPA coupling (C-2 decision).
     */
    @Column(name = "upline_id", columnDefinition = "uuid")
    private UUID uplineId;

    @Version
    private Long version;
}
