package com.omnip.transaction.domain.model;

import com.omnip.transaction.domain.model.MutationReferenceType;
import com.omnip.transaction.domain.model.MutationType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StoreMutationsTest {

    @Test
    void testStoreMutationsHasStoreIdField() {
        // Given
        StoreMutations mutations = new StoreMutations();
        UUID storeId = UUID.randomUUID();
        
        // When
        mutations.setStoreId(storeId);
        
        // Then
        assertEquals(storeId, mutations.getStoreId());
    }

    @Test
    void testStoreMutationsDoesNotHaveStoreEntityReference() {
        // Given & When
        StoreMutations mutations = new StoreMutations();
        
        // Then
        // We should be able to verify that there's no 'Store' object reference
        // Just checking that we can access storeId as UUID and not as Store entity
        UUID storeId = UUID.randomUUID();
        mutations.setStoreId(storeId);
        assertEquals(storeId, mutations.getStoreId());
        
        // Verify we can't access a Store entity object (field shouldn't exist)
        // This is more of a compile-time check, but we can at least verify
        // that the field is UUID and not a Store object
        assertNotNull(mutations.getStoreId());
        assertTrue(mutations.getStoreId() instanceof UUID);
    }

    @Test
    void testStoreMutationsFields() {
        // Given
        StoreMutations mutations = new StoreMutations();
        UUID id = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
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
        mutations.setStoreId(storeId);
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
        assertEquals(storeId, mutations.getStoreId());
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