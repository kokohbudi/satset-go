package com.satset.wallet.service;

import com.satset.wallet.repository.WalletAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletIdGenerator Tests")
class WalletIdGeneratorTest {

    @Mock
    private WalletAccountRepository walletAccountRepository;

    private WalletIdGenerator walletIdGenerator;

    @BeforeEach
    void setUp() {
        walletIdGenerator = new WalletIdGenerator(walletAccountRepository);
    }

    @Test
    @DisplayName("Should generate wallet ID with format 700xxxxxxx")
    void generate_shouldReturnFormat700xxxxxxx() {
        // Arrange
        when(walletAccountRepository.nextWalletIdSequence()).thenReturn(1L);

        // Act
        String walletId = walletIdGenerator.generate();

        // Assert
        assertThat(walletId)
                .startsWith("700")
                .hasSize(10)
                .matches("700\\d{7}");
    }

    @Test
    @DisplayName("Should generate wallet ID starting from 7000000001")
    void generate_shouldStartFromSequence1() {
        // Arrange
        when(walletAccountRepository.nextWalletIdSequence()).thenReturn(1L);

        // Act
        String walletId = walletIdGenerator.generate();

        // Assert
        assertThat(walletId).isEqualTo("7000000001");
    }

    @Test
    @DisplayName("Should generate wallet ID with padded zeros for small sequences")
    void generate_shouldPadWithZeros() {
        // Arrange
        when(walletAccountRepository.nextWalletIdSequence()).thenReturn(123L);

        // Act
        String walletId = walletIdGenerator.generate();

        // Assert
        assertThat(walletId).isEqualTo("7000000123");
    }

    @Test
    @DisplayName("Should generate unique wallet IDs for consecutive calls")
    void generate_shouldReturnUniqueIds() {
        // Arrange
        when(walletAccountRepository.nextWalletIdSequence())
                .thenReturn(1L)
                .thenReturn(2L)
                .thenReturn(3L);

        // Act
        String id1 = walletIdGenerator.generate();
        String id2 = walletIdGenerator.generate();
        String id3 = walletIdGenerator.generate();

        // Assert
        assertThat(id1).isEqualTo("7000000001");
        assertThat(id2).isEqualTo("7000000002");
        assertThat(id3).isEqualTo("7000000003");
        assertThat(Set.of(id1, id2, id3)).hasSize(3);
    }

    @Test
    @DisplayName("Should handle maximum sequence number")
    void generate_shouldHandleMaxSequence() {
        // Arrange
        when(walletAccountRepository.nextWalletIdSequence()).thenReturn(9_999_999L);

        // Act
        String walletId = walletIdGenerator.generate();

        // Assert
        assertThat(walletId).isEqualTo("7009999999");
    }

    @Test
    @DisplayName("Should be thread-safe under concurrent access")
    void generate_shouldHandleConcurrentAccess() throws InterruptedException {
        // Arrange
        int threadCount = 100;
        Set<String> generatedIds = new HashSet<>();
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // Mock sequence to return incrementing values (atomic — the real DB
        // sequence is atomic, so the test stub must be too under concurrency)
        final AtomicLong counter = new AtomicLong(0);
        when(walletAccountRepository.nextWalletIdSequence()).thenAnswer(inv -> counter.incrementAndGet());

        // Act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    String id = walletIdGenerator.generate();
                    synchronized (generatedIds) {
                        generatedIds.add(id);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertThat(generatedIds).hasSize(threadCount);
        assertThat(generatedIds).allMatch(id -> id.matches("700\\d{7}"));
    }

    @Test
    @DisplayName("Should call sequence for each generation")
    void generate_shouldCallSequence() {
        // Arrange
        when(walletAccountRepository.nextWalletIdSequence()).thenReturn(1L);

        // Act
        walletIdGenerator.generate();

        // Assert
        verify(walletAccountRepository, times(1)).nextWalletIdSequence();
    }
}
