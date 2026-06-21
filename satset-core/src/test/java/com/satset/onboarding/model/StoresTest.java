package com.satset.onboarding.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Stores Domain Model Tests")
class StoresTest {

    @Test
    @DisplayName("Should create store with basic fields")
    void shouldCreateStoreWithBasicFields() {
        // Arrange & Act
        Stores store = new Stores();
        store.setId(UUID.randomUUID());
        store.setName("Test Store");
        store.setEmail("test@example.com");
        store.setPhone("08123456789");

        // Assert
        assertThat(store.getName()).isEqualTo("Test Store");
        assertThat(store.getEmail()).isEqualTo("test@example.com");
        assertThat(store.getPhone()).isEqualTo("08123456789");
    }

    @Test
    @DisplayName("Should accept walletId field")
    void shouldAcceptWalletId() {
        // Arrange
        Stores store = new Stores();
        String walletId = "7001234567";

        // Act
        store.setWalletId(walletId);

        // Assert
        assertThat(store.getWalletId()).isEqualTo(walletId);
    }

    @Test
    @DisplayName("Should allow null walletId for existing stores")
    void shouldAllowNullWalletId() {
        // Arrange
        Stores store = new Stores();

        // Act
        store.setWalletId(null);

        // Assert
        assertThat(store.getWalletId()).isNull();
    }

    @Test
    @DisplayName("Should accept various wallet ID formats")
    void shouldAcceptVariousWalletIdFormats() {
        // Arrange
        Stores store = new Stores();

        // Act & Assert
        store.setWalletId("7000000001");
        assertThat(store.getWalletId()).isEqualTo("7000000001");

        store.setWalletId("7001234567");
        assertThat(store.getWalletId()).isEqualTo("7001234567");

        store.setWalletId("7009999999");
        assertThat(store.getWalletId()).isEqualTo("7009999999");
    }

    @Test
    @DisplayName("Should have walletId as String type")
    void shouldHaveWalletIdAsString() {
        // Arrange
        Stores store = new Stores();

        // Act
        store.setWalletId("7000000001");

        // Assert
        assertThat(store.getWalletId()).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("Should maintain other fields when setting walletId")
    void shouldMaintainOtherFields() {
        // Arrange
        Stores store = new Stores();
        UUID id = UUID.randomUUID();
        store.setId(id);
        store.setName("Test Store");
        store.setEmail("test@example.com");
        store.setActive(true);

        // Act
        store.setWalletId("7001234567");

        // Assert
        assertThat(store.getId()).isEqualTo(id);
        assertThat(store.getName()).isEqualTo("Test Store");
        assertThat(store.getEmail()).isEqualTo("test@example.com");
        assertThat(store.isActive()).isTrue();
        assertThat(store.getWalletId()).isEqualTo("7001234567");
    }
}
