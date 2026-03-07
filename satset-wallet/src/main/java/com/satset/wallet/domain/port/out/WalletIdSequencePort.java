package com.satset.wallet.domain.port.out;

/**
 * Port for generating unique wallet ID sequences.
 * Implementations should provide thread-safe sequence generation.
 */
public interface WalletIdSequencePort {

    /**
     * Returns the next sequence number for wallet ID generation.
     * Sequence starts from 1 and increments by 1 for each call.
     *
     * @return next sequence number (1 to 9,999,999)
     */
    long nextVal();
}
