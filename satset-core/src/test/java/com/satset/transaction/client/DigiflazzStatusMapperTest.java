package com.satset.transaction.client;

import com.satset.transaction.model.ProviderStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DigiflazzStatusMapperTest {

    @ParameterizedTest
    @CsvSource(value = {
            "Sukses,00,SUCCESS",
            "sukses,00,SUCCESS",
            "SUKSES,00,SUCCESS",
            "Gagal,02,FAILED",
            "Gagal,99,FAILED",
            "Gagal,01,PENDING",
            "Gagal,50,PENDING",
            "Gagal,NULL,PENDING",
            "Pending,03,PENDING",
            "Unknown,00,PENDING",
            "NULL,00,PENDING",
    }, nullValues = "NULL")
    void mapsDigiflazzStatusAndRcToProviderStatus(String dfStatus, String rc, ProviderStatus expected) {
        assertThat(DigiflazzStatusMapper.map(dfStatus, rc)).isEqualTo(expected);
    }
}
