package com.satset.wallet.domain.service;

import com.satset.wallet.domain.port.out.WalletIdSequencePort;
import org.springframework.stereotype.Service;

/**
 * Service for generating wallet IDs in bank account format.
 * Format: 700 + 7-digit sequence (e.g., 7000000001, 7001234567)
 */
@Service
public class WalletIdGenerator {

    private static final String PREFIX = "700";
    private static final int SEQUENCE_LENGTH = 7;

    private final WalletIdSequencePort sequencePort;

    public WalletIdGenerator(WalletIdSequencePort sequencePort) {
        this.sequencePort = sequencePort;
    }

    /**
     * Generates a new wallet ID.
     * Format: 700xxxxxxx where x is zero-padded sequence number
     *
     * @return unique wallet ID string (10 characters)
     */
    public String generate() {
        long sequence = sequencePort.nextVal();
        return PREFIX + String.format("%0" + SEQUENCE_LENGTH + "d", sequence);
    }
}
