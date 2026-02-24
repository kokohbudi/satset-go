package com.omnip.onboarding.domain.service;

import com.omnip.onboarding.domain.service.StoreHelper;
import com.omnip.onboarding.domain.model.Stores;
import com.omnip.onboarding.adapter.out.persistence.StoreJpaRepository;
import org.springframework.stereotype.Service;

@Service
public class StoreDomainService {
    private final StoreJpaRepository storeRepository;
    private final StoreHelper storeBusiness;

    public StoreDomainService(StoreJpaRepository storeRepository, StoreHelper storeBusiness) {
        this.storeRepository = storeRepository;
        this.storeBusiness = storeBusiness;
    }

    public Stores createNewStore(Stores stores) {
        Stores preparedStores = this.storeBusiness.prepareNewStore(stores);
        return this.storeRepository.save(preparedStores);
    }

}