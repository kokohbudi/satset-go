package com.omnip.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
public class ProductDenomMeta {
    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "product_denom_id", nullable = false)
    private ProductDenoms productDenom;

    @Column(nullable = false, length = 100)
    private String metaKey;

    @Column(columnDefinition = "text")
    private String metaValue;

    @CreatedDate
    private LocalDateTime createdAt;
}
