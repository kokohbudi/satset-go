package com.satset.wallet.service;

import com.satset.wallet.repository.WalletAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for generating wallet IDs in bank account format.
 * Format: 700 + 7-digit sequence (e.g., 7000000001, 7001234567)
 */
@Service
public class WalletIdGenerator {

    private static final String PREFIX = "700";
    private static final int SEQUENCE_LENGTH = 7;

    private final WalletAccountRepository walletAccountRepository;

    public WalletIdGenerator(WalletAccountRepository walletAccountRepository) {
        this.walletAccountRepository = walletAccountRepository;
    }

    /**
     * Generates a new wallet ID.
     * Format: 700xxxxxxx where x is zero-padded sequence number.
     *
     * <p>Runs in a separate transaction so the sequence is consumed even if the
     * caller's transaction rolls back.
     *
     * @return unique wallet ID string (10 characters)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate() {
        long sequence = walletAccountRepository.nextWalletIdSequence();
        return PREFIX + String.format("%0" + SEQUENCE_LENGTH + "d", sequence);
    }
}
