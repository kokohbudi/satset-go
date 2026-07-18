package com.satset.transaction.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Per-day counter backing the human-readable transaction ref_no. One row per WIB
 * day; {@code seq} is bumped atomically via an UPSERT in {@code RefNoGenerator}.
 * The entity exists only so Hibernate ddl-auto creates the table — increments go
 * through raw SQL, not JPA.
 */
@Entity
@Table(name = "ref_counter")
public class RefCounter {

    @Id
    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Column(name = "seq", nullable = false)
    private Long seq;
}
