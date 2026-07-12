package com.satset.identity.service;

import com.satset.identity.model.Users;
import com.satset.identity.repository.UserRepository;
import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
    private UserRepository usersRepository;

    private UserDTO sessionUserDTO;
    private UserManagementHelper helper;

    @BeforeEach
    void setUp() {
        sessionUserDTO = new UserDTO();
        sessionUserDTO.setEmail("session@mail.com");
        sessionUserDTO.setProviderUserId("kc-session");
        sessionUserDTO.setStoreId(UUID.randomUUID());

        helper = new UserManagementHelper(usersRepository);
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

    // ==================== helpers ====================

    private Users buildUser(String email, String providerId) {
        Users u = new Users();
        u.setEmail(email);
        u.setProviderUserId(providerId);
        return u;
    }
}
