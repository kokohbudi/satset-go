package com.omnip.business;

import com.omnip.entities.Store;
import org.springframework.stereotype.Component;

@Component
public class StoreBusiness {
    /**
     * Mempersiapkan store baru untuk disimpan
     */
    public Store prepareNewStore(Store store) {
        return store;
    }
}

