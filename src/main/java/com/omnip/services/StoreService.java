package com.omnip.services;

import com.omnip.business.StoreBusiness;
import com.omnip.entities.Stores;
import com.omnip.repositories.StoreRepository;
import org.springframework.stereotype.Service;

@Service
public class StoreService {
    private final StoreRepository storeRepository;
    private final StoreBusiness storeBusiness;

    public StoreService(StoreRepository storeRepository, StoreBusiness storeBusiness) {
        this.storeRepository = storeRepository;
        this.storeBusiness = storeBusiness;
    }

    public Stores createNewStore(Stores stores) {
        Stores preparedStores = this.storeBusiness.prepareNewStore(stores);
        return this.storeRepository.save(preparedStores);
    }

}