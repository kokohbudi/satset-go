package com.omnip.services;

import com.omnip.entities.Store;
import com.omnip.repositories.StoreRepository;
import org.springframework.stereotype.Service;

@Service
public class StoreService {
    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public Store createNewStore(Store store) {
        return this.storeRepository.save(store);
    }

    public Store findByEmail(String email) {
        return this.storeRepository.findByEmail(email);
    }
}
