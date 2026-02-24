package com.omnip.onboarding.domain.service;

import com.omnip.onboarding.domain.model.Stores;
import org.springframework.stereotype.Component;

@Component
public class StoreHelper {
    /**
     * Mempersiapkan store baru untuk disimpan
     */
    public Stores prepareNewStore(Stores stores) {
        return stores;
    }
}

