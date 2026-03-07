package com.omnip.transaction.domain.port.out;

import com.omnip.transaction.domain.model.WalletAccount;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletAccountPortTest {

    @Test
    void testFindByStoreIdMethodExists() {
        // Given
        WalletAccountPort port = Mockito.mock(WalletAccountPort.class);
        UUID storeId = UUID.randomUUID();
        
        // When
        when(port.findByStoreId(storeId)).thenReturn(Optional.empty());
        Optional<WalletAccount> result = port.findByStoreId(storeId);
        
        // Then
        verify(port).findByStoreId(storeId);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByStoreIdWithLockMethodExists() {
        // Given
        WalletAccountPort port = Mockito.mock(WalletAccountPort.class);
        UUID storeId = UUID.randomUUID();
        
        // When
        when(port.findByStoreIdWithLock(storeId)).thenReturn(Optional.empty());
        Optional<WalletAccount> result = port.findByStoreIdWithLock(storeId);
        
        // Then
        verify(port).findByStoreIdWithLock(storeId);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSaveMethodExists() {
        // Given
        WalletAccountPort port = Mockito.mock(WalletAccountPort.class);
        WalletAccount walletAccount = new WalletAccount(UUID.randomUUID(), null);
        
        // When
        when(port.save(walletAccount)).thenReturn(walletAccount);
        WalletAccount result = port.save(walletAccount);
        
        // Then
        verify(port).save(walletAccount);
        assertNotNull(result);
    }
}