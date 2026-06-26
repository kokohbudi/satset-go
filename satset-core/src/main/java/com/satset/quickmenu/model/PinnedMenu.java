package com.satset.quickmenu.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/** A single dashboard quick-menu pin owned by one user. */
@Entity
@Table(
        name = "pinned_menu",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pinned_user_role", columnNames = {"user_id", "role_name"}),
        indexes = @Index(name = "idx_pinned_user", columnList = "user_id"))
@EntityListeners(AuditingEntityListener.class)
@Data
public class PinnedMenu {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    /** Keycloak sub of the owning user. */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /** Keycloak role name = menu identity. */
    @Column(name = "role_name", nullable = false)
    private String roleName;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
