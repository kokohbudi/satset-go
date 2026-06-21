package com.satset.wallet.repository;

import com.satset.wallet.model.WalletAccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for wallet account entities.
 * Primary key is walletId (String, format: 700xxxxxxx).
 */
@Repository
public interface WalletAccountRepository extends JpaRepository<WalletAccountEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WalletAccountEntity w WHERE w.walletId = :walletId")
    Optional<WalletAccountEntity> findByWalletIdWithLock(@Param("walletId") String walletId);

    /**
     * Returns the next value of the wallet ID PostgreSQL sequence.
     * Sequence starts from 1 and increments by 1 for each call.
     */
    @Query(value = "SELECT nextval('satset_wallet.wallet_id_seq')", nativeQuery = true)
    long nextWalletIdSequence();
}
