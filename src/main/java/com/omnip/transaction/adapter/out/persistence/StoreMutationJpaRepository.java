package com.omnip.transaction.adapter.out.persistence;

import com.omnip.transaction.domain.model.StoreMutations;
import com.omnip.onboarding.domain.model.Stores;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreMutationJpaRepository extends JpaRepository<StoreMutations, UUID> {

    Optional<StoreMutations> findTopByStoreOrderByCreatedAtDesc(Stores store);
}
