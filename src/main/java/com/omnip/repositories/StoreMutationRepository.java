package com.omnip.repositories;

import com.omnip.entities.StoreMutations;
import com.omnip.entities.Stores;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreMutationRepository extends JpaRepository<StoreMutations, UUID> {

    Optional<StoreMutations> findTopByStoreOrderByCreatedAtDesc(Stores store);
}
