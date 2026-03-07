package com.satset.onboarding.domain.service;

import com.satset.onboarding.domain.model.Stores;
import com.satset.onboarding.domain.port.out.StoreRepositoryPort;
import com.satset.onboarding.domain.port.out.WalletCreationPort;
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
    private StoreRepositoryPort storeRepository;

    @Mock
    private WalletCreationPort walletCreationPort;

    private StoreDomainService storeDomainService;

    @BeforeEach
    void setUp() {
        storeDomainService = new StoreDomainService(storeRepository, walletCreationPort);
    }

    @Test
    @DisplayName("Should create store with auto-generated wallet")
    void createNewStore_shouldAutoCreateWallet() {
        // Arrange
        Stores store = new Stores();
        store.setName("Test Store");
        store.setEmail("test@example.com");

        String generatedWalletId = "7001234567";
        UUID storeId = UUID.randomUUID();

        when(walletCreationPort.createWallet(any(UUID.class))).thenReturn(generatedWalletId);
        when(storeRepository.save(any(Stores.class))).thenAnswer(invocation -> {
            Stores saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(storeId);
            }
            return saved;
        });

        // Act
        Stores result = storeDomainService.createNewStore(store);

        // Assert
        assertThat(result.getWalletId()).isEqualTo(generatedWalletId);
        verify(walletCreationPort).createWallet(any(UUID.class));
        verify(storeRepository, times(2)).save(any(Stores.class)); // Initial save + update with walletId
    }

    @Test
    @DisplayName("Should save store with walletId set")
    void createNewStore_shouldSaveStoreWithWalletId() {
        // Arrange
        Stores store = new Stores();
        store.setName("Test Store");
        String walletId = "7000000001";
        UUID storeId = UUID.randomUUID();

        when(walletCreationPort.createWallet(any(UUID.class))).thenReturn(walletId);
        when(storeRepository.save(any(Stores.class))).thenAnswer(invocation -> {
            Stores saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(storeId);
            }
            return saved;
        });

        // Act
        Stores result = storeDomainService.createNewStore(store);

        // Assert
        assertThat(result.getWalletId()).isEqualTo(walletId);
    }

    @Test
    @DisplayName("Should still create store when wallet creation fails")
    void createNewStore_whenWalletCreationFails_shouldStillCreateStore() {
        // Arrange
        Stores store = new Stores();
        store.setName("Test Store");

        when(walletCreationPort.createWallet(any(UUID.class)))
                .thenThrow(new RuntimeException("Wallet service unavailable"));
        when(storeRepository.save(any(Stores.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Stores result = storeDomainService.createNewStore(store);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Store");
        // Wallet ID should be null when creation fails
        assertThat(result.getWalletId()).isNull();
        verify(storeRepository, times(2)).save(any(Stores.class)); // Initial save + update attempt
    }

    @Test
    @DisplayName("Should call wallet creation with correct store ID")
    void createNewStore_shouldCallWalletCreationWithCorrectStoreId() {
        // Arrange
        Stores store = new Stores();
        store.setName("Test Store");
        UUID expectedStoreId = UUID.randomUUID();

        when(walletCreationPort.createWallet(any(UUID.class))).thenReturn("7000000001");
        when(storeRepository.save(any(Stores.class))).thenAnswer(invocation -> {
            Stores saved = invocation.getArgument(0);
            saved.setId(expectedStoreId);
            return saved;
        });

        // Act
        storeDomainService.createNewStore(store);

        // Assert
        verify(walletCreationPort).createWallet(expectedStoreId);
    }

    @Test
    @DisplayName("Should preserve all store fields when creating")
    void createNewStore_shouldPreserveAllStoreFields() {
        // Arrange
        Stores store = new Stores();
        store.setName("Test Store");
        store.setEmail("test@example.com");
        store.setPhone("08123456789");
        store.setActive(true);

        when(walletCreationPort.createWallet(any(UUID.class))).thenReturn("7000000001");
        when(storeRepository.save(any(Stores.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Stores result = storeDomainService.createNewStore(store);

        // Assert
        assertThat(result.getName()).isEqualTo("Test Store");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getPhone()).isEqualTo("08123456789");
        assertThat(result.isActive()).isTrue();
    }
}
