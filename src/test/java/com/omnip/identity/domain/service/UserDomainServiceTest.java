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

    // ==================== findByEmail / findByProviderUserId ====================

    @Test
    void findByEmail_DelegatesToRepository() {
        com.omnip.identity.domain.model.Users user = new com.omnip.identity.domain.model.Users();
        when(usersRepository.findByEmail("alice@mail.com")).thenReturn(user);

        assertSame(user, userDomainService.findByEmail("alice@mail.com"));
    }

    @Test
    void findByProviderUserId_DelegatesToRepository() {
        com.omnip.identity.domain.model.Users user = new com.omnip.identity.domain.model.Users();
        when(usersRepository.findByProviderUserId("kc-abc")).thenReturn(user);

        assertSame(user, userDomainService.findByProviderUserId("kc-abc"));
    }

    // ==================== getProviderUserIdByEmail ====================

    @Test
    void getProviderUserIdByEmail_Found_ReturnsId() throws BusinessException {
        com.omnip.identity.domain.model.Users user = new com.omnip.identity.domain.model.Users();
        user.setProviderUserId("kc-alice");
        when(usersRepository.findByEmail("alice@mail.com")).thenReturn(user);

        assertEquals("kc-alice", userDomainService.getProviderUserIdByEmail("alice@mail.com"));
    }

    @Test
    void getProviderUserIdByEmail_NotFound_ThrowsBusinessException() {
        when(usersRepository.findByEmail("unknown@mail.com")).thenReturn(null);

        assertThrows(com.omnip.shared.exception.BusinessException.class,
                () -> userDomainService.getProviderUserIdByEmail("unknown@mail.com"));
    }

    // ==================== createNewUser ====================

    @Test
    void createNewUser_DelegatesToRepository() {
        com.omnip.identity.domain.model.Users user = new com.omnip.identity.domain.model.Users();
        when(usersRepository.save(user)).thenReturn(user);

        assertSame(user, userDomainService.createNewUser(user));
    }

    // ==================== saveUserToDb ====================

    @Test
    void saveUserToDb_Success_ReturnsSuccessDTO() {
        UserDTO req = new UserDTO();
        com.omnip.identity.domain.model.Users user = new com.omnip.identity.domain.model.Users();
        UserDTO successDTO = new UserDTO();
        successDTO.setStatus("success");

        when(userManagementBusiness.createUserObject(req, "kc-new")).thenReturn(user);
        when(usersRepository.save(user)).thenReturn(user);
        when(userManagementBusiness.createSuccessResponse(req, "kc-new")).thenReturn(successDTO);

        UserDTO result = userDomainService.saveUserToDb(req, "kc-new");

        assertEquals("success", result.getStatus());
    }

    @Test
    void saveUserToDb_RepositoryThrows_ReturnsErrorDTO() {
        UserDTO req = new UserDTO();
        UserDTO errorDTO = new UserDTO();
        errorDTO.setStatus("failed");

        when(userManagementBusiness.createUserObject(any(), any())).thenThrow(new RuntimeException("DB error"));
        when(userManagementBusiness.createErrorResponse(any())).thenReturn(errorDTO);

        UserDTO result = userDomainService.saveUserToDb(req, "kc-new");

        assertEquals("failed", result.getStatus());
    }

    // ==================== updateUserStatusInDb ====================

    @Test
    void updateUserStatusInDb_DelegatesToHelper() throws com.omnip.shared.exception.BusinessException {
        doNothing().when(userManagementBusiness).setUserStatus(any(), any(), any());

        userDomainService.updateUserStatusInDb("alice@mail.com", false);

        verify(userManagementBusiness).setUserStatus(any(), any(), any());
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
