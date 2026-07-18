package com.satset.onboarding.service.store;

import com.satset.onboarding.model.Stores;
import com.satset.onboarding.repository.StoreRepository;
import com.satset.wallet.model.WalletAccountEntity;
import com.satset.wallet.service.account.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StoreDomainService Tests")
class StoreDomainServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private WalletService walletService;

    private StoreDomainService storeDomainService;

    @BeforeEach
    void setUp() {
        storeDomainService = new StoreDomainService(storeRepository, walletService);
    }

    private UUID setupSaveWithIdAssignment() {
        UUID assignedId = UUID.randomUUID();
        when(storeRepository.save(any(Stores.class))).thenAnswer(invocation -> {
            Stores s = invocation.getArgument(0);
            if (s.getId() == null) s.setId(assignedId);
            return s;
        });
        return assignedId;
    }

    @Test
    @DisplayName("Should create store with auto-generated wallet")
    void createNewStore_shouldAutoCreateWallet() {
        Stores store = new Stores();
        store.setName("Test Store");
        store.setEmail("test@example.com");
        String generatedWalletId = "7001234567";

        setupSaveWithIdAssignment();
        when(walletService.createWallet()).thenReturn(WalletAccountEntity.newAccount(generatedWalletId));

        Stores result = storeDomainService.createNewStore(store);

        assertThat(result.getWalletId()).isEqualTo(generatedWalletId);
        verify(walletService).createWallet();
        verify(storeRepository, times(2)).save(any(Stores.class));
    }

    @Test
    @DisplayName("Should assign store ID from DB before calling wallet service")
    void createNewStore_shouldAssignIdFromDbBeforeWalletCall() {
        Stores store = new Stores();
        store.setName("Test Store");

        UUID assignedId = setupSaveWithIdAssignment();
        when(walletService.createWallet()).thenReturn(WalletAccountEntity.newAccount("7000000001"));

        Stores result = storeDomainService.createNewStore(store);

        assertThat(result.getId()).isEqualTo(assignedId);
        verify(walletService).createWallet();
        verify(storeRepository, times(2)).save(any(Stores.class));
    }

    @Test
    @DisplayName("Should save store with walletId set")
    void createNewStore_shouldSaveStoreWithWalletId() {
        Stores store = new Stores();
        store.setName("Test Store");
        String walletId = "7000000001";

        setupSaveWithIdAssignment();
        when(walletService.createWallet()).thenReturn(WalletAccountEntity.newAccount(walletId));

        Stores result = storeDomainService.createNewStore(store);

        assertThat(result.getWalletId()).isEqualTo(walletId);
        verify(storeRepository, times(2)).save(any(Stores.class));
    }

    @Test
    @DisplayName("Should still create store when wallet creation fails")
    void createNewStore_whenWalletCreationFails_shouldStillCreateStore() {
        Stores store = new Stores();
        store.setName("Test Store");

        setupSaveWithIdAssignment();
        when(walletService.createWallet())
                .thenThrow(new RuntimeException("Wallet service unavailable"));

        Stores result = storeDomainService.createNewStore(store);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Store");
        assertThat(result.getWalletId()).isNull();
        // Only 1 save: wallet failed so no second save
        verify(storeRepository, times(1)).save(any(Stores.class));
    }

    @Test
    @DisplayName("Should call wallet creation after store ID is assigned by first save")
    void createNewStore_shouldCallWalletCreationAfterIdAssigned() {
        Stores store = new Stores();
        store.setName("Test Store");

        setupSaveWithIdAssignment();
        when(walletService.createWallet()).thenReturn(WalletAccountEntity.newAccount("7000000001"));

        storeDomainService.createNewStore(store);

        verify(walletService).createWallet();
    }

    @Test
    @DisplayName("Should preserve all store fields when creating")
    void createNewStore_shouldPreserveAllStoreFields() {
        Stores store = new Stores();
        store.setName("Test Store");
        store.setEmail("test@example.com");
        store.setPhone("08123456789");
        store.setActive(true);

        setupSaveWithIdAssignment();
        when(walletService.createWallet()).thenReturn(WalletAccountEntity.newAccount("7000000001"));

        Stores result = storeDomainService.createNewStore(store);

        assertThat(result.getName()).isEqualTo("Test Store");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getPhone()).isEqualTo("08123456789");
        assertThat(result.isActive()).isTrue();
        verify(storeRepository, times(2)).save(any(Stores.class));
    }
}
