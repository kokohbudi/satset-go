package com.satset.transaction.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalletAccountTest {

    @Test
    void testWalletAccountCreation() {
        UUID storeId = UUID.randomUUID();
        BigDecimal balance = new BigDecimal("100000");

        WalletAccount walletAccount = new WalletAccount(storeId, balance);

        assertThat(walletAccount.getStoreId()).isEqualTo(storeId);
        assertThat(walletAccount.getBalance()).isEqualByComparingTo(balance);
        assertThat(walletAccount.getWalletId()).isNull();
        assertThat(walletAccount.getVersion()).isNull();
    }

    @Test
    void testWalletAccountCreationWithNullBalance() {
        UUID storeId = UUID.randomUUID();

        WalletAccount walletAccount = new WalletAccount(storeId, null);

        assertThat(walletAccount.getStoreId()).isEqualTo(storeId);
        assertThat(walletAccount.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void testWalletAccountSetBalance() {
        WalletAccount walletAccount = new WalletAccount(UUID.randomUUID(), new BigDecimal("100000"));
        BigDecimal newBalance = new BigDecimal("90000");

        walletAccount.setBalance(newBalance);

        assertThat(walletAccount.getBalance()).isEqualByComparingTo(newBalance);
    }
}
