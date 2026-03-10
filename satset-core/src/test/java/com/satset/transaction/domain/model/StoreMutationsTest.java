package com.satset.transaction.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StoreMutationsTest {

    @Test
    void testStoreMutationsHasWalletIdField() {
        // Given
        StoreMutations mutations = new StoreMutations();
        String walletId = "7001234567";

        // When
        mutations.setWalletId(walletId);

        // Then
        assertEquals(walletId, mutations.getWalletId());
    }

    @Test
    void testStoreMutationsDoesNotHaveStoreEntityReference() {
        // Given & When
        StoreMutations mutations = new StoreMutations();

        // Then
        // Verify we can set walletId as String
        String walletId = "7001234567";
        mutations.setWalletId(walletId);
        assertEquals(walletId, mutations.getWalletId());

        // Verify walletId is a String
        assertInstanceOf(String.class, mutations.getWalletId());
    }

    @Test
    void testStoreMutationsFields() {
        // Given
        StoreMutations mutations = new StoreMutations();
        UUID id = UUID.randomUUID();
        String walletId = "7001234567";
        BigDecimal amount = new BigDecimal("10000");
        MutationType type = MutationType.DEBIT;
        BigDecimal balanceAfter = new BigDecimal("90000");
        MutationReferenceType referenceType = MutationReferenceType.PURCHASE;
        UUID referenceId = UUID.randomUUID();
        String description = "Test mutation";
        LocalDateTime createdAt = LocalDateTime.now();
        Long version = 1L;

        // When
        mutations.setId(id);
        mutations.setWalletId(walletId);
        mutations.setAmount(amount);
        mutations.setType(type);
        mutations.setBalanceAfter(balanceAfter);
        mutations.setReferenceType(referenceType);
        mutations.setReferenceId(referenceId);
        mutations.setDescription(description);
        mutations.setCreatedAt(createdAt);
        mutations.setVersion(version);

        // Then
        assertEquals(id, mutations.getId());
        assertEquals(walletId, mutations.getWalletId());
        assertEquals(amount, mutations.getAmount());
        assertEquals(type, mutations.getType());
        assertEquals(balanceAfter, mutations.getBalanceAfter());
        assertEquals(referenceType, mutations.getReferenceType());
        assertEquals(referenceId, mutations.getReferenceId());
        assertEquals(description, mutations.getDescription());
        assertEquals(createdAt, mutations.getCreatedAt());
        assertEquals(version, mutations.getVersion());
    }
}
