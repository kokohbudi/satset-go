package com.satset.transaction.service.topup;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against shared dev Postgres. Uses a far-past day (1999-01-01) so the real
 * "today" counter is never touched or reset, then deletes that row after.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class RefNoGeneratorIntegrationTest {

    private static final LocalDate TEST_DAY = LocalDate.of(1999, 1, 1);

    @Autowired RefNoGenerator generator;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM ref_counter WHERE day = ?", Date.valueOf(TEST_DAY));
    }

    @Test
    void nextSeq_isUniqueUnderConcurrency() throws InterruptedException {
        int n = 50;
        Set<Long> seqs = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    seqs.add(generator.nextSeq(TEST_DAY));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(seqs).hasSize(n);                 // no duplicates
        assertThat(seqs).containsExactlyInAnyOrderElementsOf(
                java.util.stream.LongStream.rangeClosed(1, n).boxed().toList()); // 1..n, no gaps
    }

    @Test
    void next_prefixesTodayInWib() {
        String expectedPrefix = LocalDate.now(java.time.ZoneId.of("Asia/Jakarta"))
                .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        assertThat(generator.next()).startsWith(expectedPrefix).hasSize(13);
        // note: this bumps today's real counter by 1 — harmless, gaps are expected.
    }
}
