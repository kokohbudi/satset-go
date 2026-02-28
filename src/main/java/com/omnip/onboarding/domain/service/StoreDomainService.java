package com.omnip.onboarding.domain.service;

import com.omnip.onboarding.domain.service.StoreHelper;
import com.omnip.onboarding.domain.model.Stores;
import com.omnip.onboarding.domain.port.in.CreateStoreUseCase;
import com.omnip.onboarding.domain.port.out.StoreRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class StoreDomainService implements CreateStoreUseCase {
    private final StoreRepositoryPort storeRepository;
    private final StoreHelper storeBusiness;

    public StoreDomainService(StoreRepositoryPort storeRepository, StoreHelper storeBusiness) {
        this.storeRepository = storeRepository;
        this.storeBusiness = storeBusiness;
    }

    public Stores createNewStore(Stores stores) {
        Stores preparedStores = this.storeBusiness.prepareNewStore(stores);
        return this.storeRepository.save(preparedStores);
    }

}