package com.omnip.business;

import com.omnip.entities.Stores;
import org.springframework.stereotype.Component;

@Component
public class StoreBusiness {
    /**
     * Mempersiapkan store baru untuk disimpan
     */
    public Stores prepareNewStore(Stores stores) {
        return stores;
    }
}

