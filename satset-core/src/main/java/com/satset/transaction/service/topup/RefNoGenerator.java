package com.satset.transaction.service.topup;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Generates the outward-facing transaction ref_no: {@code YYYYMMDD} (WIB day) +
 * a 5-digit daily counter. Internal DB access only — no {@code @LogContext}
 * (that rule is for outbound supplier calls, not own-DB writes).
 */
@Service
public class RefNoGenerator {

    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");
    private final JdbcTemplate jdbc;

    public RefNoGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Full ref_no for today (WIB). REQUIRES_NEW so this proxy boundary — not the
     * internal self-invoked {@link #nextSeq(LocalDate)} call — is where the isolated
     * transaction is actually established (plain self-invocation bypasses the Spring
     * AOP proxy, so {@code @Transactional} on {@code nextSeq} alone has no effect
     * when reached via this method).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next() {
        LocalDate day = LocalDate.now(WIB);
        return format(day, nextSeq(day));
    }

    /**
     * Atomic per-day counter bump. REQUIRES_NEW so the counter-row lock is held
     * only for this UPSERT and released on its own commit — NOT across the caller's
     * provider HTTP call. Gaps (when the caller's outer tx later rolls back) are
     * acceptable; a duplicate seq would corrupt Digiflazz idempotency, a gap won't.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long nextSeq(LocalDate day) {
        Long seq = jdbc.queryForObject("""
                INSERT INTO ref_counter(day, seq) VALUES (?, 1)
                ON CONFLICT (day) DO UPDATE SET seq = ref_counter.seq + 1
                RETURNING seq
                """, Long.class, Date.valueOf(day));
        return seq;
    }

    static String format(LocalDate day, long seq) {
        return day.format(DateTimeFormatter.BASIC_ISO_DATE) + String.format("%05d", seq);
    }
}
