package com.satset.onboarding.domain.port.in;

import com.satset.onboarding.domain.model.Stores;

/**
 * Input port for store creation.
 */
public interface CreateStoreUseCase {

    Stores createNewStore(Stores stores);
}
