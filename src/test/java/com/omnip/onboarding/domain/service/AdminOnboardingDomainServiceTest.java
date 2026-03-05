package com.omnip.onboarding.domain.service;

import com.omnip.identity.domain.model.Users;
import com.omnip.onboarding.domain.model.Stores;
import com.omnip.onboarding.domain.port.out.KeycloakOrganizationPort;
import com.omnip.onboarding.domain.port.out.OnboardingUserPort;
import com.omnip.onboarding.domain.port.out.StoreRepositoryPort;
import com.omnip.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminOnboardingDomainServiceTest {

    @Mock
    private KeycloakOrganizationPort keycloakOrgPort;

    @Mock
    private StoreRepositoryPort storeRepository;

    @Mock
    private OnboardingUserPort usersRepository;

    @InjectMocks
    private AdminOnboardingDomainService service;

    private static final String USERNAME = "john";
    private static final String EMAIL = "john@mail.com";
    private static final String ORG_NAME = "John's Store";
    private static final String PHONE = "081234567890";
    private static final String KC_ORG_ID = "kc-org-uuid";
    private static final String KC_USER_ID = "kc-user-uuid";

    @BeforeEach
    void setUp() throws Exception {
        when(keycloakOrgPort.userExistsByEmail(EMAIL)).thenReturn(false);
        when(keycloakOrgPort.createOrganization(ORG_NAME)).thenReturn(KC_ORG_ID);
        when(keycloakOrgPort.createResellerUser(USERNAME, USERNAME, EMAIL)).thenReturn(KC_USER_ID);
        when(storeRepository.save(any())).thenAnswer(inv -> {
            Stores s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(usersRepository.findByProviderUserId(KC_USER_ID)).thenReturn(null);
        when(usersRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void onboardReseller_Success_HappyPath() throws Exception {
        service.onboardReseller(USERNAME, EMAIL, ORG_NAME, PHONE, null);

        verify(keycloakOrgPort).createOrganization(ORG_NAME);
        verify(keycloakOrgPort).createResellerUser(USERNAME, USERNAME, EMAIL);
        verify(keycloakOrgPort).addMemberToOrganization(KC_ORG_ID, KC_USER_ID);
        verify(keycloakOrgPort).assignClientRoleToUser(KC_USER_ID, "org_owner");
        verify(storeRepository).save(any());
        verify(usersRepository).save(any());
    }

    @Test
    void onboardReseller_DuplicateEmail_ThrowsBusinessException() throws Exception {
        when(keycloakOrgPort.userExistsByEmail(EMAIL)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.onboardReseller(USERNAME, EMAIL, ORG_NAME, PHONE, null));

        assertTrue(ex.getMessage().contains(EMAIL));
        verify(keycloakOrgPort, never()).createOrganization(any());
        verify(storeRepository, never()).save(any());
    }

    @Test
    void onboardReseller_CreateResellerUserFails_ThrowsGenericBusinessException() throws Exception {
        when(keycloakOrgPort.createResellerUser(any(), any(), any()))
                .thenThrow(new RuntimeException("KC timeout"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.onboardReseller(USERNAME, EMAIL, ORG_NAME, PHONE, null));

        assertEquals("Gagal membuat reseller. Silakan coba lagi.", ex.getMessage());
        verify(storeRepository, never()).save(any());
    }

    @Test
    void onboardReseller_NullUplineStoreId_NoUplineLinked() throws Exception {
        service.onboardReseller(USERNAME, EMAIL, ORG_NAME, PHONE, null);

        ArgumentCaptor<Stores> storeCaptor = ArgumentCaptor.forClass(Stores.class);
        verify(storeRepository).save(storeCaptor.capture());
        assertNull(storeCaptor.getValue().getUpline());
        verify(storeRepository, never()).findById(any());
    }

    @Test
    void onboardReseller_WithUplineStoreId_LinksUpline() throws Exception {
        UUID uplineId = UUID.randomUUID();
        Stores uplineStore = new Stores();
        uplineStore.setId(uplineId);

        when(storeRepository.findById(uplineId)).thenReturn(Optional.of(uplineStore));

        service.onboardReseller(USERNAME, EMAIL, ORG_NAME, PHONE, uplineId.toString());

        ArgumentCaptor<Stores> storeCaptor = ArgumentCaptor.forClass(Stores.class);
        verify(storeRepository).save(storeCaptor.capture());
        assertSame(uplineStore, storeCaptor.getValue().getUpline());
    }

    @Test
    void onboardReseller_ExistingUserInDB_SkipsNewUserCreation() throws Exception {
        Users existingUser = new Users();
        existingUser.setProviderUserId(KC_USER_ID);
        existingUser.setEmail(EMAIL);

        when(usersRepository.findByProviderUserId(KC_USER_ID)).thenReturn(existingUser);

        service.onboardReseller(USERNAME, EMAIL, ORG_NAME, PHONE, null);

        ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
        verify(usersRepository).save(userCaptor.capture());
        // existing user reused — providerUserId unchanged
        assertEquals(KC_USER_ID, userCaptor.getValue().getProviderUserId());
    }

    @Test
    void onboardReseller_CreateOrganizationFails_PropagatesException() throws Exception {
        when(keycloakOrgPort.createOrganization(ORG_NAME))
                .thenThrow(new BusinessException("KC org error"));

        assertThrows(BusinessException.class,
                () -> service.onboardReseller(USERNAME, EMAIL, ORG_NAME, PHONE, null));

        verify(keycloakOrgPort, never()).createResellerUser(any(), any(), any());
        verify(storeRepository, never()).save(any());
    }

    @Test
    void onboardReseller_AssignClientRoleFails_ThrowsGenericBusinessException() throws Exception {
        doThrow(new RuntimeException("KC role error"))
                .when(keycloakOrgPort).assignClientRoleToUser(any(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.onboardReseller(USERNAME, EMAIL, ORG_NAME, PHONE, null));

        assertEquals("Gagal membuat reseller. Silakan coba lagi.", ex.getMessage());
        verify(storeRepository, never()).save(any());
    }

    @Test
    void onboardReseller_StoreHasCorrectFields() throws Exception {
        service.onboardReseller(USERNAME, EMAIL, ORG_NAME, PHONE, null);

        ArgumentCaptor<Stores> storeCaptor = ArgumentCaptor.forClass(Stores.class);
        verify(storeRepository).save(storeCaptor.capture());
        Stores saved = storeCaptor.getValue();

        assertEquals(ORG_NAME, saved.getName());
        assertEquals(PHONE, saved.getPhone());
        assertEquals(EMAIL, saved.getEmail());
        assertEquals(KC_ORG_ID, saved.getKeycloakOrganizationId());
        assertTrue(saved.isActive());
        assertFalse(saved.isDeleted());
        assertNotNull(saved.getReferralId());
    }
}
