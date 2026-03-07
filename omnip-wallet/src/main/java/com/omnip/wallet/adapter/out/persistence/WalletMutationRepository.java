package com.omnip.wallet.adapter.out.persistence;

import com.omnip.wallet.adapter.out.persistence.entity.WalletMutationEntity;
import com.omnip.wallet.domain.model.MutationReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletMutationRepository extends JpaRepository<WalletMutationEntity, UUID> {

    List<WalletMutationEntity> findByStoreIdOrderByCreatedAtDesc(UUID storeId);

    Optional<WalletMutationEntity> findByReferenceIdAndReferenceType(UUID referenceId, MutationReferenceType referenceType);
}