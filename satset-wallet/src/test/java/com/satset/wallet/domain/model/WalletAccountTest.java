package com.satset.wallet.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WalletAccount Domain Model Tests")
class WalletAccountTest {

    @Test
    @DisplayName("Should create new account with walletId and zero balance")
    void newAccount_shouldCreateWithWalletId() {
        // Arrange
        String walletId = "7000000001";

        // Act
        WalletAccount account = WalletAccount.newAccount(walletId);

        // Assert
        assertThat(account.walletId()).isEqualTo(walletId);
        assertThat(account.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.version()).isNull();
    }

    @Test
    @DisplayName("Should create new account with zero balance")
    void newAccount_shouldHaveZeroBalance() {
        // Arrange
        String walletId = "7000000001";

        // Act
        WalletAccount account = WalletAccount.newAccount(walletId);

        // Assert
        assertThat(account.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should update balance with new value")
    void withBalance_shouldReturnNewAccountWithUpdatedBalance() {
        // Arrange
        String walletId = "7000000001";
        WalletAccount account = WalletAccount.newAccount(walletId);
        BigDecimal newBalance = new BigDecimal("150000.00");

        // Act
        WalletAccount updated = account.withBalance(newBalance);

        // Assert
        assertThat(updated.walletId()).isEqualTo(walletId);
        assertThat(updated.balance()).isEqualByComparingTo(newBalance);
        assertThat(updated.version()).isNull();
    }

    @Test
    @DisplayName("Should preserve walletId when updating balance")
    void withBalance_shouldPreserveWalletId() {
        // Arrange
        String walletId = "7001234567";
        WalletAccount account = WalletAccount.newAccount(walletId);

        // Act
        WalletAccount updated = account.withBalance(new BigDecimal("50000"));

        // Assert
        assertThat(updated.walletId()).isEqualTo(walletId);
    }

    @Test
    @DisplayName("Should accept various wallet ID formats")
    void newAccount_shouldAcceptVariousWalletIdFormats() {
        // Act & Assert
        assertThat(WalletAccount.newAccount("7000000001").walletId()).isEqualTo("7000000001");
        assertThat(WalletAccount.newAccount("7001234567").walletId()).isEqualTo("7001234567");
        assertThat(WalletAccount.newAccount("7009999999").walletId()).isEqualTo("7009999999");
    }

    @Test
    @DisplayName("Should be immutable")
    void shouldBeImmutable() {
        // Arrange
        String walletId = "7000000001";
        WalletAccount account = WalletAccount.newAccount(walletId);

        // Act
        WalletAccount updated = account.withBalance(new BigDecimal("1000"));

        // Assert - original should not change
        assertThat(account.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updated.balance()).isEqualByComparingTo(new BigDecimal("1000"));
    }
}
