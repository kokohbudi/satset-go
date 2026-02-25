package com.omnip.identity.domain.service;

import com.omnip.identity.adapter.in.web.dto.ChangeMyPasswordRequestDTO;
import com.omnip.identity.domain.port.out.KeycloakIdentityPort;
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
        ChangeMyPasswordRequestDTO req = new ChangeMyPasswordRequestDTO();
        req.setOldPassword("oldPass");
        req.setNewPassword("newPass123");
        req.setConfirmPassword("mismatchPass");

        assertThrows(IllegalArgumentException.class, () ->
            service.changeMyPassword("user123", "test@test.com", req));
    }

    @Test
    void changeMyPassword_ValidatesOldPassword() {
        ChangeMyPasswordRequestDTO req = new ChangeMyPasswordRequestDTO();
        req.setOldPassword("wrongOld");
        req.setNewPassword("newPass123");
        req.setConfirmPassword("newPass123");

        when(keycloakPort.verifyUserPassword("test@test.com", "wrongOld")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
            service.changeMyPassword("user123", "test@test.com", req));
    }

    @Test
    void changeMyPassword_Success() {
        ChangeMyPasswordRequestDTO req = new ChangeMyPasswordRequestDTO();
        req.setOldPassword("correctOld");
        req.setNewPassword("newPass123");
        req.setConfirmPassword("newPass123");

        when(keycloakPort.verifyUserPassword("test@test.com", "correctOld")).thenReturn(true);

        service.changeMyPassword("user123", "test@test.com", req);

        verify(keycloakPort).changeUserPassword("user123", "newPass123");
    }
}
