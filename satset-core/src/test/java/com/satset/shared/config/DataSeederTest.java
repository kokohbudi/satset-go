package com.satset.shared.config;

import com.satset.catalog.domain.port.out.CategoryRepositoryPort;
import com.satset.catalog.domain.port.out.DenomMetaRepositoryPort;
import com.satset.catalog.domain.port.out.DenomRepositoryPort;
import com.satset.catalog.domain.port.out.ProductRepositoryPort;
import com.satset.onboarding.domain.model.Stores;
import com.satset.onboarding.adapter.out.persistence.StoreRepository;
import com.satset.onboarding.domain.port.out.WalletCreationPort;
import com.satset.transaction.domain.model.WalletAccount;
import com.satset.transaction.domain.port.out.WalletAccountPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private CategoryRepositoryPort categoryRepository;

    @Mock
    private ProductRepositoryPort productRepository;

    @Mock
    private DenomRepositoryPort denomRepository;

    @Mock
    private DenomMetaRepositoryPort metaRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private WalletAccountPort walletAccountPort;

    @Mock
    private WalletCreationPort walletCreationPort;

    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        dataSeeder = new DataSeeder(
                categoryRepository,
                productRepository,
                denomRepository,
                metaRepository,
                storeRepository,
                walletAccountPort,
                walletCreationPort
        );
    }

    // ========================================
    // seedWalletAccounts() Tests
    // ========================================

    @Test
    void seedWalletAccounts_whenWalletAccountsAlreadyExist_shouldSkip() {
        // Given
        when(walletAccountPort.count()).thenReturn(5L);

        // When
        dataSeeder.seedWalletAccounts();

        // Then
        verify(walletAccountPort, never()).findByWalletId(any());
        verify(walletAccountPort, never()).save(any());
    }

    @Test
    void seedWalletAccounts_whenNoStores_shouldSkip() {
        // Given
        when(walletAccountPort.count()).thenReturn(0L);
        when(storeRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        dataSeeder.seedWalletAccounts();

        // Then
        verify(walletAccountPort, never()).save(any());
    }

    @Test
    void seedWalletAccounts_whenStoresWithNoWalletId_shouldCreateWalletAccounts() {
        // Given
        UUID storeId1 = UUID.randomUUID();
        UUID storeId2 = UUID.randomUUID();

        Stores store1 = new Stores();
        store1.setId(storeId1);
        store1.setWalletId(null);

        Stores store2 = new Stores();
        store2.setId(storeId2);
        store2.setWalletId(null);

        when(walletAccountPort.count()).thenReturn(0L);
        when(storeRepository.findAll()).thenReturn(Arrays.asList(store1, store2));

        // When
        dataSeeder.seedWalletAccounts();

        // Then — walletId is null, so createWallet is called for both
        verify(walletCreationPort, times(2)).createWallet(any());
    }

    @Test
    void seedWalletAccounts_whenWalletAccountAlreadyExists_shouldSkipThatStore() {
        // Given
        UUID storeId1 = UUID.randomUUID();
        UUID storeId2 = UUID.randomUUID();
        String walletId1 = "7001000001";
        String walletId2 = "7001000002";

        Stores store1 = new Stores();
        store1.setId(storeId1);
        store1.setWalletId(walletId1);

        Stores store2 = new Stores();
        store2.setId(storeId2);
        store2.setWalletId(walletId2);

        WalletAccount existingWallet = new WalletAccount(walletId1, new BigDecimal("100000.00"));

        when(walletAccountPort.count()).thenReturn(0L);
        when(storeRepository.findAll()).thenReturn(Arrays.asList(store1, store2));
        when(walletAccountPort.findByWalletId(walletId1)).thenReturn(Optional.of(existingWallet));
        when(walletAccountPort.findByWalletId(walletId2)).thenReturn(Optional.empty());

        // When
        dataSeeder.seedWalletAccounts();

        // Then
        verify(walletAccountPort).findByWalletId(walletId1);
        verify(walletAccountPort).findByWalletId(walletId2);
        // Only store2 should trigger createWallet (store1 already has wallet)
        verify(walletCreationPort, times(1)).createWallet(storeId2);
    }

    @Test
    void seedWalletAccounts_whenStoreHasWalletIdButNoWalletAccount_shouldCallCreateWallet() {
        // Given
        UUID storeId = UUID.randomUUID();
        String walletId = "7001000001";

        Stores store = new Stores();
        store.setId(storeId);
        store.setWalletId(walletId);

        when(walletAccountPort.count()).thenReturn(0L);
        when(storeRepository.findAll()).thenReturn(Collections.singletonList(store));
        when(walletAccountPort.findByWalletId(walletId)).thenReturn(Optional.empty());

        // When
        dataSeeder.seedWalletAccounts();

        // Then
        verify(walletCreationPort).createWallet(storeId);
    }
}
