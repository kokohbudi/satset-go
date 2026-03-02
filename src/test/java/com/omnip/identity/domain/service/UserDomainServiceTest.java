package com.omnip.identity.domain.service;

import com.omnip.identity.domain.port.out.KeycloakIdentityPort;
import com.omnip.identity.domain.port.out.UserRepositoryPort;
import com.omnip.shared.dto.UserDTO;
import com.omnip.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDomainServiceTest {

    @Mock
    private UserRepositoryPort usersRepository;

    @Mock
    private UserManagementHelper userManagementBusiness;

    @Mock
    private UserDTO sessionUserDTO;

    @Mock
    private KeycloakIdentityPort keycloakAdminClientService;

    @InjectMocks
    private UserDomainService userDomainService;

    private UserDTO requestUserDTO;
    private final String MOCK_PROVIDER_USER_ID = "mock-keycloak-id-123";

    @BeforeEach
    void setUp() {
        requestUserDTO = new UserDTO();
        requestUserDTO.setEmail("test@example.com");
        requestUserDTO.setPassword("newPassword123!");
    }

    @Test
    void shouldChangePasswordSuccessfully() throws BusinessException {
        // Arrange
        when(userManagementBusiness.getProviderUseIdChangePassword(sessionUserDTO, requestUserDTO))
                .thenReturn(MOCK_PROVIDER_USER_ID);
        doNothing().when(keycloakAdminClientService).changeUserPassword(MOCK_PROVIDER_USER_ID,
                requestUserDTO.getPassword());

        // Act
        UserDTO result = userDomainService.changePassword(requestUserDTO);

        // Assert
        assertEquals("success", result.getStatus());
        verify(userManagementBusiness, times(1)).getProviderUseIdChangePassword(sessionUserDTO, requestUserDTO);
        verify(keycloakAdminClientService, times(1)).changeUserPassword(MOCK_PROVIDER_USER_ID,
                requestUserDTO.getPassword());
    }

    @Test
    void shouldFailChangePasswordWhenValidationFails() throws BusinessException {
        // Arrange
        String errorMessage = "User validation failed";
        when(userManagementBusiness.getProviderUseIdChangePassword(sessionUserDTO, requestUserDTO))
                .thenThrow(new BusinessException(errorMessage));

        // Act
        UserDTO result = userDomainService.changePassword(requestUserDTO);

        // Assert
        assertEquals("failed", result.getStatus());
        assertEquals(errorMessage, result.getMessage());
        verify(userManagementBusiness, times(1)).getProviderUseIdChangePassword(sessionUserDTO, requestUserDTO);
        verify(keycloakAdminClientService, never()).changeUserPassword(anyString(), anyString());
    }
}
