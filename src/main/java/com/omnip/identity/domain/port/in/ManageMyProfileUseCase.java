package com.omnip.identity.domain.port.in;

import com.omnip.identity.adapter.in.web.dto.ChangeMyPasswordRequestDTO;

public interface ManageMyProfileUseCase {
    void changeMyPassword(String providerUserId, String email, ChangeMyPasswordRequestDTO requestDTO);
}
