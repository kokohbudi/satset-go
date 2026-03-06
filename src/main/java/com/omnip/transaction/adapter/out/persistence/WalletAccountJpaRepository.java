package com.omnip.transaction.adapter.out.persistence;

import com.omnip.transaction.domain.model.WalletAccount;
import com.omnip.transaction.domain.port.out.WalletAccountPort;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletAccountJpaRepository extends JpaRepository<WalletAccount, UUID>, WalletAccountPort {

    Optional<WalletAccount> findByStoreId(UUID storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WalletAccount w WHERE w.storeId = :storeId")
    Optional<WalletAccount> findByStoreIdWithLock(@Param("storeId") UUID storeId);
}
