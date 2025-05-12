package com.omnip.services;

import com.omnip.business.StoreBusiness;
import com.omnip.entities.Store;
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

    public Store createNewStore(Store store) {
        Store preparedStore = this.storeBusiness.prepareNewStore(store);
        return this.storeRepository.save(preparedStore);
    }

}