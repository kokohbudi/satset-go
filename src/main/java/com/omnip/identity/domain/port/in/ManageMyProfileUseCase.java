package com.omnip.identity.domain.port.in;

import com.omnip.identity.domain.model.ChangeMyPasswordRequest;

public interface ManageMyProfileUseCase {
    void changeMyPassword(String providerUserId, String email, ChangeMyPasswordRequest request);
}
