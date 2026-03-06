package com.omnip.onboarding.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stores")
@EntityListeners(AuditingEntityListener.class)
@Data
public class StoreJpaEntity {
    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
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
    @Column(precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

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

    @ManyToOne
    @JoinColumn(name = "upline_id")
    private StoreJpaEntity upline;

    @Version
    private Long version;
}