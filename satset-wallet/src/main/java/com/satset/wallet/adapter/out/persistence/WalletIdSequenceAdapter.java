package com.satset.wallet.adapter.out.persistence;

import com.satset.wallet.domain.port.out.WalletIdSequencePort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter for wallet ID sequence generation using PostgreSQL sequence.
 * Uses separate transaction to ensure sequence is consumed even if main transaction rolls back.
 */
@Component
public class WalletIdSequenceAdapter implements WalletIdSequencePort {

    private static final String SEQUENCE_NAME = "satset_wallet.wallet_id_seq";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long nextVal() {
        Number result = (Number) entityManager
                .createNativeQuery("SELECT nextval(:sequenceName)")
                .setParameter("sequenceName", SEQUENCE_NAME)
                .getSingleResult();
        return result.longValue();
    }
}
