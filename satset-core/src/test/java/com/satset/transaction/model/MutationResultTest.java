package com.satset.transaction.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MutationResultTest {

    @Test
    void testMutationResultCreation() {
        UUID mutationId = UUID.randomUUID();
        BigDecimal balanceAfter = new BigDecimal("90000");

        MutationResult result = new MutationResult(mutationId, balanceAfter);

        assertThat(result.mutationId()).isEqualTo(mutationId);
        assertThat(result.balanceAfter()).isEqualByComparingTo(balanceAfter);
    }
}
