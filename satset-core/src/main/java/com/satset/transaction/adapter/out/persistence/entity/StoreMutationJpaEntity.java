package com.satset.transaction.adapter.out.persistence.entity;

import com.satset.transaction.domain.model.MutationReferenceType;
import com.satset.transaction.domain.model.MutationType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "store_mutations",
    uniqueConstraints = @UniqueConstraint(
        name = "uc_storemutation_storeid_referenceid",
        columnNames = {"store_id", "reference_id"}
    )
)
@EntityListeners(AuditingEntityListener.class)
@Data
public class StoreMutationJpaEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "store_id", nullable = false, columnDefinition = "uuid")
    private UUID storeId;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MutationType type;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MutationReferenceType referenceType;

    @Column(columnDefinition = "uuid")
    private UUID referenceId;

    @Column(length = 255)
    private String description;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;
}