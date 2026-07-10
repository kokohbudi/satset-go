package com.satset.catalog.service;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class CatalogCodeUtilTest {
    @Test void normalizes() {
        assertThat(CatalogCodeUtil.toCode("E-Money")).isEqualTo("EMONEY");
        assertThat(CatalogCodeUtil.toCode("Mobile Legends")).isEqualTo("MOBILELEGENDS");
        assertThat(CatalogCodeUtil.toCode(" Pulsa ")).isEqualTo("PULSA");
        assertThat(CatalogCodeUtil.toCode("by.U")).isEqualTo("BYU");
    }
}
