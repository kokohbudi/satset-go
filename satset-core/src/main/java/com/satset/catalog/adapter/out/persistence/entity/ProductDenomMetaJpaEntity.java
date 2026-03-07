package com.satset.catalog.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_denom_meta")
@EntityListeners(AuditingEntityListener.class)
@Data
public class ProductDenomMetaJpaEntity {
    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "product_denom_id", nullable = false, columnDefinition = "uuid")
    private UUID productDenomId;

    @Column(nullable = false, length = 100)
    private String metaKey;

    @Column(columnDefinition = "text")
    private String metaValue;

    @CreatedDate
    private LocalDateTime createdAt;
}