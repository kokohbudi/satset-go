package com.satset.identity.domain.port.in;

import com.satset.identity.domain.model.ChangeMyPasswordRequest;

public interface ManageMyProfileUseCase {
    void changeMyPassword(String providerUserId, String email, ChangeMyPasswordRequest request);
}
