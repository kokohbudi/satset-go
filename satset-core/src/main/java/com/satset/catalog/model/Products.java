package com.satset.catalog.model;

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

@Entity
@Table(name = "products", uniqueConstraints =
        @UniqueConstraint(name = "uq_products_category_code", columnNames = {"category_id", "code"}))
@EntityListeners(AuditingEntityListener.class)
@Data
public class Products {
    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "category_id", nullable = false, columnDefinition = "uuid")
    private UUID categoryId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String providerName;

    @Column(columnDefinition = "text")
    private String description;

    private String iconUrl;

    private boolean active;

    private boolean deleted;

    /** true = masih ada di supplier; di-set false oleh sync saat item hilang dari supplier. */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean inSupplier = true;

    private int sortOrder;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    @Version
    private Long version;
}
