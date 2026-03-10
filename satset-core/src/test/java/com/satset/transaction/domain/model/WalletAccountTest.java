package com.satset.transaction.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WalletAccountTest {

    @Test
    void testWalletAccountCreation() {
        String walletId = "7001234567";
        BigDecimal balance = new BigDecimal("100000");

        WalletAccount walletAccount = new WalletAccount(walletId, balance);

        assertThat(walletAccount.getWalletId()).isEqualTo(walletId);
        assertThat(walletAccount.getBalance()).isEqualByComparingTo(balance);
        assertThat(walletAccount.getVersion()).isNull();
    }

    @Test
    void testWalletAccountCreationWithNullBalance() {
        String walletId = "7001234567";

        WalletAccount walletAccount = new WalletAccount(walletId, null);

        assertThat(walletAccount.getWalletId()).isEqualTo(walletId);
        assertThat(walletAccount.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void testWalletAccountSetBalance() {
        WalletAccount walletAccount = new WalletAccount("7001234567", new BigDecimal("100000"));
        BigDecimal newBalance = new BigDecimal("90000");

        walletAccount.setBalance(newBalance);

        assertThat(walletAccount.getBalance()).isEqualByComparingTo(newBalance);
    }
}
