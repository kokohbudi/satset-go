package com.satset.identity.service.user;

import com.satset.identity.repository.UserRepository;
import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.BusinessException;
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
    private UserRepository usersRepository;

    @Mock
    private UserManagementHelper userManagementBusiness;

    @Mock
    private UserDTO sessionUserDTO;

    @InjectMocks
    private UserDomainService userDomainService;

    // ==================== getProviderUserIdByEmail ====================

    @Test
    void getProviderUserIdByEmail_Found_ReturnsId() throws BusinessException {
        com.satset.identity.model.Users user = new com.satset.identity.model.Users();
        user.setProviderUserId("kc-alice");
        when(usersRepository.findByEmail("alice@mail.com")).thenReturn(user);

        assertEquals("kc-alice", userDomainService.getProviderUserIdByEmail("alice@mail.com"));
    }

    @Test
    void getProviderUserIdByEmail_NotFound_ThrowsBusinessException() {
        when(usersRepository.findByEmail("unknown@mail.com")).thenReturn(null);

        assertThrows(com.satset.shared.exception.BusinessException.class,
                () -> userDomainService.getProviderUserIdByEmail("unknown@mail.com"));
    }

    // ==================== updateUserStatusInDb ====================

    @Test
    void updateUserStatusInDb_DelegatesToHelper() throws com.satset.shared.exception.BusinessException {
        doNothing().when(userManagementBusiness).setUserStatus(any(), any(), any());

        userDomainService.updateUserStatusInDb("alice@mail.com", false);

        verify(userManagementBusiness).setUserStatus(any(), any(), any());
    }
}
