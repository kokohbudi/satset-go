package com.satset.onboarding.domain.service;

import com.satset.identity.domain.model.Users;
import com.satset.onboarding.domain.model.Stores;
import com.satset.onboarding.domain.port.out.OnboardingUserPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationDomainServiceTest {

    @Mock
    private OnboardingUserPort onboardingUserPort;

    @Mock
    private StoreDomainService storeService;

    @Mock
    private RegistrationHelper registrationBusiness;

    @InjectMocks
    private RegistrationDomainService registrationDomainService;

    // === isEmailRegistered ===

    @Test
    void isEmailRegistered_DelegatesToHelper_ReturnsTrue() {
        when(registrationBusiness.isEmailRegistered("test@mail.com")).thenReturn(true);

        assertTrue(registrationDomainService.isEmailRegistered("test@mail.com"));

        verify(registrationBusiness).isEmailRegistered("test@mail.com");
    }

    @Test
    void isEmailRegistered_DelegatesToHelper_ReturnsFalse() {
        when(registrationBusiness.isEmailRegistered("new@mail.com")).thenReturn(false);

        assertFalse(registrationDomainService.isEmailRegistered("new@mail.com"));
    }

    // === registerNewStore ===

    @Test
    void registerNewStore_Success_CallsAllStepsInOrder() {
        String email = "owner@mail.com";
        String fullName = "John Doe";
        List<String> roles = List.of("reseller");

        Stores preparedStore = new Stores();
        Stores savedStore = new Stores();
        savedStore.setId(UUID.randomUUID());

        Users preparedUser = new Users();
        Users savedUser = new Users();
        savedUser.setEmail(email);

        when(registrationBusiness.prepareNewStore(email, fullName)).thenReturn(preparedStore);
        when(storeService.createNewStore(preparedStore)).thenReturn(savedStore);
        when(registrationBusiness.prepareNewStoreUser(email, fullName, roles, "WEB", "kc-123", savedStore))
                .thenReturn(preparedUser);
        when(onboardingUserPort.save(preparedUser)).thenReturn(savedUser);
        when(registrationBusiness.createRegistrationResponse(savedStore, savedUser))
                .thenReturn(Map.of("store", savedStore, "user", savedUser));

        Map<String, Object> result = registrationDomainService.registerNewStore(
                email, fullName, roles, "WEB", "kc-123");

        assertNotNull(result);
        assertEquals(savedStore, result.get("store"));
        assertEquals(savedUser, result.get("user"));

        var inOrder = inOrder(registrationBusiness, storeService, onboardingUserPort);
        inOrder.verify(registrationBusiness).prepareNewStore(email, fullName);
        inOrder.verify(storeService).createNewStore(preparedStore);
        inOrder.verify(registrationBusiness).prepareNewStoreUser(email, fullName, roles, "WEB", "kc-123", savedStore);
        inOrder.verify(onboardingUserPort).save(preparedUser);
        inOrder.verify(registrationBusiness).createRegistrationResponse(savedStore, savedUser);
    }

    @Test
    void registerNewStore_UserSavedWithReturnedUserFromPort() {
        Stores preparedStore = new Stores();
        Stores savedStore = new Stores();
        Users preparedUser = new Users();
        Users savedUser = new Users();
        savedUser.setProviderUserId("kc-saved");

        when(registrationBusiness.prepareNewStore(any(), any())).thenReturn(preparedStore);
        when(storeService.createNewStore(any())).thenReturn(savedStore);
        when(registrationBusiness.prepareNewStoreUser(any(), any(), any(), any(), any(), any()))
                .thenReturn(preparedUser);
        when(onboardingUserPort.save(preparedUser)).thenReturn(savedUser);
        when(registrationBusiness.createRegistrationResponse(any(), any()))
                .thenAnswer(inv -> Map.of("user", inv.getArgument(1)));

        Map<String, Object> result = registrationDomainService.registerNewStore(
                "e@mail.com", "Name", List.of(), "APP", "kc-abc");

        Users userInResponse = (Users) result.get("user");
        assertEquals("kc-saved", userInResponse.getProviderUserId());
    }
}
