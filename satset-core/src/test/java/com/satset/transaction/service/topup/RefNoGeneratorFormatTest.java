package com.satset.transaction.service.topup;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class RefNoGeneratorFormatTest {

    @Test
    void format_padsCounterToFiveDigits() {
        assertThat(RefNoGenerator.format(LocalDate.of(2026, 7, 18), 1))
                .isEqualTo("2026071800001");
    }

    @Test
    void format_atDailyCap() {
        assertThat(RefNoGenerator.format(LocalDate.of(2026, 7, 18), 99999))
                .isEqualTo("2026071899999");
    }

    @Test
    void format_beyondCapGrowsWidthDoesNotBreak() {
        // %05d is a floor, not a ceiling: seq 100000 renders as 6 digits, no crash.
        assertThat(RefNoGenerator.format(LocalDate.of(2026, 7, 18), 100000))
                .isEqualTo("20260718100000");
    }
}
