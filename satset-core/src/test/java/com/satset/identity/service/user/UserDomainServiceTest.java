package com.satset.identity.service.user;

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
class UserDomainServiceTest {

    @Mock
    private UserRepository usersRepository;

    private UserDTO sessionUserDTO;
    private UserDomainService userDomainService;

    @BeforeEach
    void setUp() {
        sessionUserDTO = new UserDTO();
        sessionUserDTO.setEmail("session@mail.com");
        sessionUserDTO.setProviderUserId("kc-session");
        sessionUserDTO.setStoreId(UUID.randomUUID());

        userDomainService = new UserDomainService(usersRepository, sessionUserDTO);
    }

    // ==================== getProviderUserIdByEmail ====================

    @Test
    void getProviderUserIdByEmail_Found_ReturnsId() throws BusinessException {
        Users user = new Users();
        user.setProviderUserId("kc-alice");
        when(usersRepository.findByEmail("alice@mail.com")).thenReturn(user);

        assertEquals("kc-alice", userDomainService.getProviderUserIdByEmail("alice@mail.com"));
    }

    @Test
    void getProviderUserIdByEmail_NotFound_ThrowsBusinessException() {
        when(usersRepository.findByEmail("unknown@mail.com")).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> userDomainService.getProviderUserIdByEmail("unknown@mail.com"));
    }

    // ==================== updateUserStatusInDb ====================

    @Test
    void updateUserStatusInDb_ChangesActiveFlag_SavesUser() throws BusinessException {
        Users sessionUser = buildUser("session@mail.com", "kc-session");
        sessionUser.setActive(true);
        Users targetUser = buildUser("other@mail.com", "kc-other");
        targetUser.setActive(true); // current active

        when(usersRepository.findByEmailInAndStoreId(anyList(), anyString()))
                .thenReturn(List.of(sessionUser, targetUser));
        when(usersRepository.save(any())).thenReturn(targetUser);

        userDomainService.updateUserStatusInDb("other@mail.com", false);

        assertFalse(targetUser.isActive());
        verify(usersRepository).save(targetUser);
    }

    @Test
    void updateUserStatusInDb_AlreadyActive_DoesNotSave() throws BusinessException {
        Users sessionUser = buildUser("session@mail.com", "kc-session");
        sessionUser.setActive(true);
        Users targetUser = buildUser("other@mail.com", "kc-other");
        targetUser.setActive(true); // already active

        when(usersRepository.findByEmailInAndStoreId(anyList(), anyString()))
                .thenReturn(List.of(sessionUser, targetUser));

        userDomainService.updateUserStatusInDb("other@mail.com", true); // request active (no change needed)

        verify(usersRepository, never()).save(any());
    }

    private Users buildUser(String email, String providerId) {
        Users u = new Users();
        u.setEmail(email);
        u.setProviderUserId(providerId);
        return u;
    }
}
