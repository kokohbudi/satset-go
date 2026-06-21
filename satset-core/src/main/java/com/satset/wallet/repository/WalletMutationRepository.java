package com.satset.wallet.repository;

import com.satset.wallet.model.WalletMutationEntity;
import com.satset.wallet.model.MutationReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletMutationRepository extends JpaRepository<WalletMutationEntity, UUID> {

    List<WalletMutationEntity> findByWalletIdOrderByCreatedAtDesc(String walletId);

    Optional<WalletMutationEntity> findByReferenceIdAndReferenceType(UUID referenceId, MutationReferenceType referenceType);
}
