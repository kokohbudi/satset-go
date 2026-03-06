package com.omnip.onboarding.domain.service;

import com.omnip.onboarding.domain.model.Stores;
import com.omnip.onboarding.domain.port.in.CreateStoreUseCase;
import com.omnip.onboarding.domain.port.out.StoreRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class StoreDomainService implements CreateStoreUseCase {
    private final StoreRepositoryPort storeRepository;

    public StoreDomainService(StoreRepositoryPort storeRepository) {
        this.storeRepository = storeRepository;
    }

    public Stores createNewStore(Stores stores) {
        return this.storeRepository.save(stores);
    }

}