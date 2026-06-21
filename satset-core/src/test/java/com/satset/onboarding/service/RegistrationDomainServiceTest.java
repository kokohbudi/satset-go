package com.satset.onboarding.service;

import com.satset.identity.model.Users;
import com.satset.onboarding.model.Stores;
import com.satset.identity.repository.UserRepository;
import com.satset.onboarding.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationDomainServiceTest {

    @Mock
    private UserRepository usersRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private RegistrationDomainService registrationDomainService;

    // === isEmailRegistered ===

    @Test
    void isEmailRegistered_UserExists_ReturnsTrue() {
        when(usersRepository.findByEmail("alice@mail.com")).thenReturn(new Users());

        assertTrue(registrationDomainService.isEmailRegistered("alice@mail.com"));
    }

    @Test
    void isEmailRegistered_StoreExists_ReturnsTrue() {
        when(usersRepository.findByEmail("alice@mail.com")).thenReturn(null);
        when(storeRepository.findByEmail("alice@mail.com")).thenReturn(new Stores());

        assertTrue(registrationDomainService.isEmailRegistered("alice@mail.com"));
    }

    @Test
    void isEmailRegistered_NeitherExists_ReturnsFalse() {
        when(usersRepository.findByEmail("new@mail.com")).thenReturn(null);
        when(storeRepository.findByEmail("new@mail.com")).thenReturn(null);

        assertFalse(registrationDomainService.isEmailRegistered("new@mail.com"));
    }
}
