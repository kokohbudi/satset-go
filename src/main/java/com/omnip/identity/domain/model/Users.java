package com.omnip.identity.domain.model;

import com.omnip.shared.converter.StringListConverter;
import com.omnip.onboarding.domain.model.Stores;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
public class Users {
    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;
    private String email;
    private String username;
    private String fullname;
    private String registrationChannel;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
    @LastModifiedBy
    private String updatedBy;
    @CreatedBy
    private String createdBy;
    private boolean active;
    private boolean deleted;
    private String providerUserId;
    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Stores stores;

    @Convert(converter = StringListConverter.class)
    @Column(name = "roles", columnDefinition = "text")
    private List<String> roles;


}
