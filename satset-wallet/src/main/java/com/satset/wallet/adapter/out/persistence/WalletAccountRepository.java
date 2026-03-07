package com.satset.wallet.adapter.out.persistence;

import com.satset.wallet.adapter.out.persistence.entity.WalletAccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletAccountRepository extends JpaRepository<WalletAccountEntity, UUID> {

    Optional<WalletAccountEntity> findByStoreId(UUID storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WalletAccountEntity w WHERE w.storeId = :storeId")
    Optional<WalletAccountEntity> findByStoreIdWithLock(@Param("storeId") UUID storeId);
}