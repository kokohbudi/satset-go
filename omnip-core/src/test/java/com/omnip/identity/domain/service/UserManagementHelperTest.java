package com.omnip.identity.domain.service;

import com.omnip.identity.domain.model.Users;
import com.omnip.identity.domain.port.out.UserRepositoryPort;
import com.omnip.onboarding.domain.model.Stores;
import com.omnip.shared.dto.UserDTO;
import com.omnip.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserManagementHelperTest {

    @Mock
    private UserRepositoryPort usersRepository;

    private UserDTO sessionUserDTO;
    private UserManagementHelper helper;

    @BeforeEach
    void setUp() {
        sessionUserDTO = new UserDTO();
        sessionUserDTO.setEmail("session@mail.com");
        sessionUserDTO.setProviderUserId("kc-session");
        sessionUserDTO.setStoreId(UUID.randomUUID());

        helper = new UserManagementHelper(sessionUserDTO, usersRepository);
    }

    // ==================== getProviderUseIdChangePassword ====================

    @Test
    void getProviderUseIdChangePassword_NullEmail_ReturnsSelf() throws BusinessException {
        UserDTO req = new UserDTO();
        req.setEmail(null); // self change

        String result = helper.getProviderUseIdChangePassword(sessionUserDTO, req);

        assertEquals("kc-session", result);
        verify(usersRepository, never()).findByEmailInAndStoreId(anyList(), anyString());
    }

    @Test
    void getProviderUseIdChangePassword_SameEmail_ReturnsSelf() throws BusinessException {
        UserDTO req = new UserDTO();
        req.setEmail("session@mail.com"); // same as session

        String result = helper.getProviderUseIdChangePassword(sessionUserDTO, req);

        assertEquals("kc-session", result);
    }

    @Test
    void getProviderUseIdChangePassword_DifferentEmail_ReturnsTargetProviderUserId() throws BusinessException {
        Users sessionUser = buildUser("session@mail.com", "kc-session");
        Users targetUser = buildUser("other@mail.com", "kc-other");
        when(usersRepository.findByEmailInAndStoreId(anyList(), anyString()))
                .thenReturn(List.of(sessionUser, targetUser));

        UserDTO req = new UserDTO();
        req.setEmail("other@mail.com");

        String result = helper.getProviderUseIdChangePassword(sessionUserDTO, req);

        assertEquals("kc-other", result);
    }

    @Test
    void getProviderUseIdChangePassword_DifferentStoreUser_ThrowsBusinessException() {
        // Only 1 user found (not 2) means they're not in the same store
        when(usersRepository.findByEmailInAndStoreId(anyList(), anyString()))
                .thenReturn(List.of(buildUser("session@mail.com", "kc-session")));

        UserDTO req = new UserDTO();
        req.setEmail("outsider@mail.com");

        assertThrows(BusinessException.class,
                () -> helper.getProviderUseIdChangePassword(sessionUserDTO, req));
    }

    // ==================== setUserStatus ====================

    @Test
    void setUserStatus_ChangesActiveFlag_SavesUser() throws BusinessException {
        Users sessionUser = buildUser("session@mail.com", "kc-session");
        sessionUser.setActive(true);
        Users targetUser = buildUser("other@mail.com", "kc-other");
        targetUser.setActive(true); // current active

        when(usersRepository.findByEmailInAndStoreId(anyList(), anyString()))
                .thenReturn(List.of(sessionUser, targetUser));
        when(usersRepository.save(any())).thenReturn(targetUser);

        UserDTO req = new UserDTO();
        req.setEmail("other@mail.com");
        req.setActive(false); // set to inactive

        helper.setUserStatus(sessionUserDTO, req, usersRepository);

        assertFalse(targetUser.isActive());
        verify(usersRepository).save(targetUser);
    }

    @Test
    void setUserStatus_AlreadyActive_DoesNotSave() throws BusinessException {
        Users sessionUser = buildUser("session@mail.com", "kc-session");
        sessionUser.setActive(true);
        Users targetUser = buildUser("other@mail.com", "kc-other");
        targetUser.setActive(true); // already active

        when(usersRepository.findByEmailInAndStoreId(anyList(), anyString()))
                .thenReturn(List.of(sessionUser, targetUser));

        UserDTO req = new UserDTO();
        req.setEmail("other@mail.com");
        req.setActive(true); // request to set active (no change needed)

        helper.setUserStatus(sessionUserDTO, req, usersRepository);

        verify(usersRepository, never()).save(any());
    }

    // ==================== createUserObject ====================

    @Test
    void createUserObject_SetsAllFieldsFromDTO() {
        UserDTO reqDTO = new UserDTO();
        reqDTO.setEmail("new@mail.com");
        reqDTO.setUsername("new");
        reqDTO.setFullname("New User");
        reqDTO.setRoles(List.of("reseller"));

        Users user = helper.createUserObject(reqDTO, "kc-new");

        assertEquals("new@mail.com", user.getEmail());
        assertEquals("new", user.getUsername());
        assertEquals("New User", user.getFullname());
        assertEquals(List.of("reseller"), user.getRoles());
        assertEquals("kc-new", user.getProviderUserId());
        assertEquals("omnia", user.getRegistrationChannel());
        assertNotNull(user.getStoreId()); // sessionUserDTO has storeId
    }

    // ==================== createSuccessResponse ====================

    @Test
    void createSuccessResponse_SetsProviderIdAndClearsPassword() {
        UserDTO dto = new UserDTO();
        dto.setPassword("secret");

        UserDTO result = helper.createSuccessResponse(dto, "kc-abc");

        assertEquals("kc-abc", result.getProviderUserId());
        assertNull(result.getPassword());
        assertEquals("success", result.getStatus());
    }

    // ==================== createErrorResponse ====================

    @Test
    void createErrorResponse_ReturnsFailedDTOWithMessage() {
        UserDTO result = helper.createErrorResponse("Something went wrong");

        assertEquals("failed", result.getStatus());
        assertEquals("Something went wrong", result.getMessage());
    }

    // ==================== helpers ====================

    private Users buildUser(String email, String providerId) {
        Users u = new Users();
        u.setEmail(email);
        u.setProviderUserId(providerId);
        return u;
    }
}
