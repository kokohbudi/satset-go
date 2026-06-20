package com.satset.identity.service;

import com.satset.identity.model.ChangeMyPasswordRequest;
import com.satset.identity.client.KeycloakIdentityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSelfServiceDomainServiceTest {

    @Mock
    private KeycloakIdentityPort keycloakPort;

    private UserSelfServiceDomainService service;

    @BeforeEach
    void setUp() {
        service = new UserSelfServiceDomainService(keycloakPort);
    }

    @Test
    void changeMyPassword_ValidatesPasswordsMatch() {
        ChangeMyPasswordRequest req = new ChangeMyPasswordRequest();
        req.setNewPassword("newPass123");
        req.setConfirmPassword("mismatchPass");

        assertThrows(IllegalArgumentException.class, () ->
            service.changeMyPassword("user123", "test@test.com", req));

        verify(keycloakPort, never()).changeUserPassword(any(), any());
    }

    @Test
    void changeMyPassword_Success() {
        ChangeMyPasswordRequest req = new ChangeMyPasswordRequest();
        req.setNewPassword("newPass123");
        req.setConfirmPassword("newPass123");

        service.changeMyPassword("user123", "test@test.com", req);

        verify(keycloakPort).changeUserPassword("user123", "newPass123");
    }
}
