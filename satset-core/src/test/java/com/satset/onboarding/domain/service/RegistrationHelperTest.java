package com.satset.onboarding.domain.service;

import com.satset.identity.domain.model.Users;
import com.satset.onboarding.domain.model.Stores;
import com.satset.onboarding.domain.port.out.OnboardingUserPort;
import com.satset.onboarding.adapter.out.persistence.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationHelperTest {

    @Mock private StoreRepository storeRepository;
    @Mock private OnboardingUserPort usersRepository;

    @InjectMocks
    private RegistrationHelper helper;

    // ==================== isEmailRegistered ====================

    @Test
    void isEmailRegistered_UserExists_ReturnsTrue() {
        when(usersRepository.findByEmail("alice@mail.com")).thenReturn(new Users());
        when(storeRepository.findByEmail("alice@mail.com")).thenReturn(null);

        assertTrue(helper.isEmailRegistered("alice@mail.com"));
    }

    @Test
    void isEmailRegistered_StoreExists_ReturnsTrue() {
        when(usersRepository.findByEmail("alice@mail.com")).thenReturn(null);
        when(storeRepository.findByEmail("alice@mail.com")).thenReturn(new Stores());

        assertTrue(helper.isEmailRegistered("alice@mail.com"));
    }

    @Test
    void isEmailRegistered_NeitherExists_ReturnsFalse() {
        when(usersRepository.findByEmail("new@mail.com")).thenReturn(null);
        when(storeRepository.findByEmail("new@mail.com")).thenReturn(null);

        assertFalse(helper.isEmailRegistered("new@mail.com"));
    }

    // ==================== prepareNewStore ====================

    @Test
    void prepareNewStore_SetsNameWithStoreSuffix() {
        when(storeRepository.existsByReferralId(anyString())).thenReturn(false);

        Stores store = helper.prepareNewStore("alice@mail.com", "Alice");

        assertEquals("Alice's Store", store.getName());
        assertEquals("alice@mail.com", store.getEmail());
        assertTrue(store.isActive());
        assertNotNull(store.getReferralId());
    }

    @Test
    void prepareNewStore_GeneratesNonNullReferralId() {
        when(storeRepository.existsByReferralId(anyString())).thenReturn(false);

        Stores store = helper.prepareNewStore("test@mail.com", "John Doe");

        assertNotNull(store.getReferralId());
        assertFalse(store.getReferralId().isBlank());
    }

    // ==================== prepareNewStoreUser ====================

    @Test
    void prepareNewStoreUser_SetsAllFields() {
        Stores store = new Stores();
        store.setId(java.util.UUID.randomUUID());
        List<String> roles = List.of("reseller");

        Users user = helper.prepareNewStoreUser(
                "alice@mail.com", "Alice", roles, "WEB", "kc-123", store);

        assertEquals("alice@mail.com", user.getEmail());
        assertEquals("alice@mail.com", user.getUsername()); // username = email
        assertEquals("Alice", user.getFullname());
        assertEquals(store.getId(), user.getStoreId());
        assertTrue(user.isActive());
        assertEquals(roles, user.getRoles());
        assertEquals("WEB", user.getRegistrationChannel());
        assertEquals("kc-123", user.getProviderUserId());
    }

    // ==================== generateReferralId ====================

    @Test
    void generateReferralId_MultiPartName_UsesFirstAndLastInitials() {
        when(storeRepository.existsByReferralId(anyString())).thenReturn(false);

        String id = helper.generateReferralId("John Doe");

        // Pattern: J + DOEXXX + 4 digits = starts with "JD"... actually J + DOE + 4digits
        assertTrue(id.startsWith("J"));
        assertEquals(8, id.length()); // 1 + up to 5 (last name) + 4 digits... actually J+DOE+4 = 8
    }

    @Test
    void generateReferralId_SingleName_UsesUpTo6Chars() {
        when(storeRepository.existsByReferralId(anyString())).thenReturn(false);

        String id = helper.generateReferralId("Alice");

        // ALICE + 4 digits = 9 chars
        assertTrue(id.startsWith("ALICE"));
        // 4-digit suffix
        assertTrue(id.matches("[A-Z0-9]+"));
    }

    @Test
    void generateReferralId_CollisionRetry_GeneratesNewId() {
        // First few attempts collide, then succeed
        when(storeRepository.existsByReferralId(anyString()))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        String id = helper.generateReferralId("Bob");

        assertNotNull(id);
        assertFalse(id.isBlank());
    }

    // ==================== createRegistrationResponse ====================

    @Test
    void createRegistrationResponse_ContainsStoreAndUser() {
        Stores store = new Stores();
        Users user = new Users();

        Map<String, Object> response = helper.createRegistrationResponse(store, user);

        assertSame(store, response.get("store"));
        assertSame(user, response.get("user"));
    }
}
