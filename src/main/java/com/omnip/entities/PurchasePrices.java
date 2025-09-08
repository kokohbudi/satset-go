package com.omnip.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Entity
@Data
@EntityListeners(AuditingEntityListener.class)
public class PurchasePrices {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;
    private boolean active;
    private boolean deleted;
    @CreatedDate
    private Date createdDate;
    @LastModifiedDate
    private Date updatedDate;
    private String updatedBy;
    @CreatedBy
    private String createdBy;
    @LastModifiedBy
    private String lastModifiedBy;
    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "vouchers_id")
    private Vouchers vouchers;
    private BigDecimal price;
    private Date effectiveDate;

}
