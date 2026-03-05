package com.omnip.onboarding.domain.service;

import com.omnip.onboarding.domain.model.Stores;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoreHelperTest {

    private final StoreHelper helper = new StoreHelper();

    @Test
    void prepareNewStore_ReturnsSameInstance() {
        Stores stores = new Stores();
        stores.setName("Test Store");

        Stores result = helper.prepareNewStore(stores);

        assertSame(stores, result);
        assertEquals("Test Store", result.getName());
    }

    @Test
    void prepareNewStore_NullStore_ReturnsNull() {
        Stores result = helper.prepareNewStore(null);

        assertNull(result);
    }
}
