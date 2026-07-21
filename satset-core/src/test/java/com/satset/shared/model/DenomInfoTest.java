package com.satset.shared.model;

import com.satset.catalog.model.DenomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DenomInfoTest {

    @Test
    void carriesPostpaidDiscriminators() {
        DenomInfo d = new DenomInfo(UUID.randomUUID(), "gopay", "GoPay Saldo", "GoPay",
                BigDecimal.ZERO, new BigDecimal("1000"), BigDecimal.ZERO, true, false,
                true, DenomType.OPEN_AMOUNT, new BigDecimal("10000"), new BigDecimal("1000000"));

        assertThat(d.requiresInquiry()).isTrue();
        assertThat(d.denomType()).isEqualTo(DenomType.OPEN_AMOUNT);
        assertThat(d.minAmount()).isEqualByComparingTo("10000");
        assertThat(d.maxAmount()).isEqualByComparingTo("1000000");
        assertThat(d.isAvailable()).isTrue();
    }
}
