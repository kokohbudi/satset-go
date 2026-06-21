package com.satset.wallet.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WalletAccountEntity Tests")
class WalletAccountEntityTest {

    @Test
    @DisplayName("Should create new account with walletId and zero balance")
    void newAccount_shouldCreateWithWalletId() {
        // Arrange
        String walletId = "7000000001";

        // Act
        WalletAccountEntity account = WalletAccountEntity.newAccount(walletId);

        // Assert
        assertThat(account.getWalletId()).isEqualTo(walletId);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getVersion()).isNull();
    }

    @Test
    @DisplayName("Should create new account with zero balance")
    void newAccount_shouldHaveZeroBalance() {
        // Arrange
        String walletId = "7000000001";

        // Act
        WalletAccountEntity account = WalletAccountEntity.newAccount(walletId);

        // Assert
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should update balance with new value")
    void setBalance_shouldUpdateBalance() {
        // Arrange
        String walletId = "7000000001";
        WalletAccountEntity account = WalletAccountEntity.newAccount(walletId);
        BigDecimal newBalance = new BigDecimal("150000.00");

        // Act
        account.setBalance(newBalance);

        // Assert
        assertThat(account.getWalletId()).isEqualTo(walletId);
        assertThat(account.getBalance()).isEqualByComparingTo(newBalance);
    }

    @Test
    @DisplayName("Should accept various wallet ID formats")
    void newAccount_shouldAcceptVariousWalletIdFormats() {
        // Act & Assert
        assertThat(WalletAccountEntity.newAccount("7000000001").getWalletId()).isEqualTo("7000000001");
        assertThat(WalletAccountEntity.newAccount("7001234567").getWalletId()).isEqualTo("7001234567");
        assertThat(WalletAccountEntity.newAccount("7009999999").getWalletId()).isEqualTo("7009999999");
    }
}
