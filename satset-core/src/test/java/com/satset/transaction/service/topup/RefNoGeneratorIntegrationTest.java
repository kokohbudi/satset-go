package com.satset.transaction.service.topup;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against shared dev Postgres. The sequence's atomicity is Postgres's
 * guarantee, not ours — so we only check ref_no is prefixed with today (WIB)
 * and that consecutive calls differ.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class RefNoGeneratorIntegrationTest {

    @Autowired RefNoGenerator generator;

    @Test
    void next_isPrefixedWithTodayAndUnique() {
        String prefix = LocalDate.now(ZoneId.of("Asia/Jakarta"))
                .format(DateTimeFormatter.BASIC_ISO_DATE);
        String a = generator.next();
        String b = generator.next();
        assertThat(a).startsWith(prefix);
        assertThat(b).startsWith(prefix);
        assertThat(a).isNotEqualTo(b);
    }
}
