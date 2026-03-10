package com.satset.transaction.domain.port.out;

import com.satset.transaction.domain.model.WalletAccount;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletAccountPortTest {

    @Test
    void testFindByWalletIdMethodExists() {
        // Given
        WalletAccountPort port = Mockito.mock(WalletAccountPort.class);
        String walletId = "7001234567";

        // When
        when(port.findByWalletId(walletId)).thenReturn(Optional.empty());
        Optional<WalletAccount> result = port.findByWalletId(walletId);

        // Then
        verify(port).findByWalletId(walletId);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByWalletIdWithLockMethodExists() {
        // Given
        WalletAccountPort port = Mockito.mock(WalletAccountPort.class);
        String walletId = "7001234567";

        // When
        when(port.findByWalletIdWithLock(walletId)).thenReturn(Optional.empty());
        Optional<WalletAccount> result = port.findByWalletIdWithLock(walletId);

        // Then
        verify(port).findByWalletIdWithLock(walletId);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSaveMethodExists() {
        // Given
        WalletAccountPort port = Mockito.mock(WalletAccountPort.class);
        WalletAccount walletAccount = new WalletAccount("7001234567", null);

        // When
        when(port.save(walletAccount)).thenReturn(walletAccount);
        WalletAccount result = port.save(walletAccount);

        // Then
        verify(port).save(walletAccount);
        assertNotNull(result);
    }
}
