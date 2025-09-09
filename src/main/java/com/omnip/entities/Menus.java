package com.omnip.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity untuk menyimpan informasi menu/permission yang dapat diakses oleh role tertentu.
 * Menu ini akan digunakan untuk mengatur akses ke fitur-fitur aplikasi.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(exclude = {"parentMenu", "subMenus"})
@ToString(exclude = {"parentMenu", "subMenus"})
public class Menus {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String menuCode;

    @Column(nullable = false)
    private String menuName;

    private String description;

    @Column(nullable = false)
    private String url;

    private String icon;

    private Integer sortOrder;

    @ManyToOne
    @JoinColumn(name = "parent_menu_id")
    private Menus parentMenu;

    @OneToMany(mappedBy = "parentMenu", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Menus> subMenus;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @LastModifiedBy
    private String updatedBy;

    @CreatedBy
    private String createdBy;

    private boolean active = true;

    private boolean deleted = false;

    @Version
    private Long version;
}
