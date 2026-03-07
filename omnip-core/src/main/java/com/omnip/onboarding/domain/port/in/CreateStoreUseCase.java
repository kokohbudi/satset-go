package com.omnip.onboarding.domain.port.in;

import com.omnip.onboarding.domain.model.Stores;

/**
 * Input port for store creation.
 */
public interface CreateStoreUseCase {

    Stores createNewStore(Stores stores);
}
