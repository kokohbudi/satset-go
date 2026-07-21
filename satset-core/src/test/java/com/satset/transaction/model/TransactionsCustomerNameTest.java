package com.satset.transaction.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionsCustomerNameTest {

    @Test
    void customerNameIsNullableAndSettable() {
        Transactions tx = new Transactions();
        assertThat(tx.getCustomerName()).isNull();
        tx.setCustomerName("BUDI SANTOSO");
        assertThat(tx.getCustomerName()).isEqualTo("BUDI SANTOSO");
    }
}
