package com.satset.transaction.service.topup;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Generates the outward-facing transaction ref_no: {@code YYYYMMDD} (WIB day) +
 * a zero-padded running counter from a Postgres sequence.
 *
 * <p>ponytail: a bare {@code nextval} sequence, not a date-keyed counter table.
 * Postgres sequences are atomic and non-transactional (a rolled-back purchase
 * still advances it — gaps are fine, uniqueness is guaranteed), so there is no
 * lock to hold and no transaction plumbing. The counter does NOT reset per day;
 * the date prefix keeps ref_no readable, the sequence keeps it globally unique.
 * Switch to a per-day counter table only if CS actually needs a 1-based daily
 * sequence.
 */
@Service
public class RefNoGenerator {

    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");
    private final JdbcTemplate jdbc;

    public RefNoGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void ensureSequence() {
        // idempotent — no manual prod DDL for the counter
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS tx_ref_seq");
    }

    public String next() {
        long n = jdbc.queryForObject("SELECT nextval('tx_ref_seq')", Long.class);
        return format(LocalDate.now(WIB), n);
    }

    static String format(LocalDate day, long seq) {
        return day.format(DateTimeFormatter.BASIC_ISO_DATE) + String.format("%05d", seq);
    }
}
