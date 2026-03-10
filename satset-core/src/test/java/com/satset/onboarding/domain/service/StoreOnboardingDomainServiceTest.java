package com.satset.onboarding.domain.service;

import com.satset.identity.domain.model.Users;
import com.satset.onboarding.domain.model.Stores;
import com.satset.onboarding.domain.port.in.CreateStoreUseCase;
import com.satset.onboarding.domain.port.out.KeycloakOrganizationPort;
import com.satset.onboarding.domain.port.out.OnboardingUserPort;
import com.satset.onboarding.domain.port.out.StoreRepositoryPort;
import com.satset.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StoreOnboardingDomainServiceTest {

    @Mock
    private KeycloakOrganizationPort keycloakOrgPort;

    @Mock
    private StoreRepositoryPort storeRepository;

    @Mock
    private OnboardingUserPort usersRepository;

    @Mock
    private CreateStoreUseCase createStoreUseCase;

    @InjectMocks
    private StoreOnboardingDomainService service;

    private static final String USER_ID = "kc-user-uuid";
    private static final String ORG_NAME = "My Store";
    private static final String PHONE = "081234567890";
    private static final String KC_ORG_ID = "kc-org-uuid";

    @BeforeEach
    void setUp() throws Exception {
        // Provide a real request context so session attribute code executes
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));

        when(keycloakOrgPort.createOrganization(ORG_NAME)).thenReturn(KC_ORG_ID);
        when(createStoreUseCase.createNewStore(any())).thenAnswer(inv -> {
            Stores s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(usersRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void onboardStore_Success_HappyPath() throws Exception {
        Users user = buildUser(USER_ID, "owner@mail.com");
        when(usersRepository.findByProviderUserId(USER_ID)).thenReturn(user);

        service.onboardStore(USER_ID, ORG_NAME, PHONE);

        verify(keycloakOrgPort).createOrganization(ORG_NAME);
        verify(keycloakOrgPort).addMemberToOrganization(KC_ORG_ID, USER_ID);
        verify(keycloakOrgPort).assignClientRoleToUser(USER_ID, "org_owner");
        verify(createStoreUseCase).createNewStore(any());
        verify(usersRepository).save(any());
    }

    @Test
    void onboardStore_UserNotFoundInDB_ThrowsGenericBusinessException() throws Exception {
        when(usersRepository.findByProviderUserId(USER_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.onboardStore(USER_ID, ORG_NAME, PHONE));

        assertEquals("Gagal mendaftarkan toko. Silakan coba lagi.", ex.getMessage());
        verify(createStoreUseCase, never()).createNewStore(any());
    }

    @Test
    void onboardStore_AddMemberFails_ThrowsGenericBusinessException() throws Exception {
        doThrow(new RuntimeException("KC error"))
                .when(keycloakOrgPort).addMemberToOrganization(any(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.onboardStore(USER_ID, ORG_NAME, PHONE));

        assertEquals("Gagal mendaftarkan toko. Silakan coba lagi.", ex.getMessage());
    }

    @Test
    void onboardStore_CreateOrganizationFails_PropagatesException() throws Exception {
        when(keycloakOrgPort.createOrganization(ORG_NAME))
                .thenThrow(new BusinessException("KC org error"));

        assertThrows(BusinessException.class,
                () -> service.onboardStore(USER_ID, ORG_NAME, PHONE));

        verify(createStoreUseCase, never()).createNewStore(any());
        verify(usersRepository, never()).save(any());
    }

    @Test
    void onboardStore_AssignClientRoleFails_ThrowsGenericBusinessException() throws Exception {
        Users user = buildUser(USER_ID, "owner@mail.com");
        when(usersRepository.findByProviderUserId(USER_ID)).thenReturn(user);
        doThrow(new RuntimeException("KC role error"))
                .when(keycloakOrgPort).assignClientRoleToUser(any(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.onboardStore(USER_ID, ORG_NAME, PHONE));

        assertEquals("Gagal mendaftarkan toko. Silakan coba lagi.", ex.getMessage());
        verify(createStoreUseCase, never()).createNewStore(any());
    }

    @Test
    void onboardStore_StoreLinkedToUser() throws Exception {
        Users user = buildUser(USER_ID, "owner@mail.com");
        when(usersRepository.findByProviderUserId(USER_ID)).thenReturn(user);

        service.onboardStore(USER_ID, ORG_NAME, PHONE);

        ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
        verify(usersRepository).save(userCaptor.capture());
        assertNotNull(userCaptor.getValue().getStoreId());
    }

    @Test
    void onboardStore_StoreEmailFromUser() throws Exception {
        Users user = buildUser(USER_ID, "owner@mail.com");
        when(usersRepository.findByProviderUserId(USER_ID)).thenReturn(user);

        service.onboardStore(USER_ID, ORG_NAME, PHONE);

        ArgumentCaptor<Stores> storeCaptor = ArgumentCaptor.forClass(Stores.class);
        verify(createStoreUseCase).createNewStore(storeCaptor.capture());
        Stores saved = storeCaptor.getValue();

        assertEquals(ORG_NAME, saved.getName());
        assertEquals(PHONE, saved.getPhone());
        assertEquals("owner@mail.com", saved.getEmail());
        assertEquals(KC_ORG_ID, saved.getKeycloakOrganizationId());
        assertTrue(saved.isActive());
        assertFalse(saved.isDeleted());
        assertNotNull(saved.getReferralId());
    }

    private Users buildUser(String providerUserId, String email) {
        Users user = new Users();
        user.setProviderUserId(providerUserId);
        user.setEmail(email);
        return user;
    }
}
