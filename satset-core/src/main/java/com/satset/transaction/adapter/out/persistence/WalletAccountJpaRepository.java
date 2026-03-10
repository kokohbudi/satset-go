package com.satset.transaction.adapter.out.persistence;

import com.satset.transaction.adapter.out.persistence.entity.WalletAccountJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletAccountJpaRepository extends JpaRepository<WalletAccountJpaEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WalletAccountJpaEntity w WHERE w.walletId = :walletId")
    Optional<WalletAccountJpaEntity> findByWalletIdWithLock(@Param("walletId") String walletId);
}
