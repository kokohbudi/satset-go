package com.omnip.identity.domain.service;

import com.omnip.identity.domain.model.ChangeMyPasswordRequest;
import com.omnip.identity.domain.port.in.ManageMyProfileUseCase;
import com.omnip.identity.domain.port.out.KeycloakIdentityPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSelfServiceDomainService implements ManageMyProfileUseCase {

    private final KeycloakIdentityPort keycloakPort;

    @Override
    public void changeMyPassword(String providerUserId, String email, ChangeMyPasswordRequest request) {
        if (!request.isPasswordMatch()) {
            throw new IllegalArgumentException("Password baru dan konfirmasi tidak cocok");
        }

        // Old password verification is skipped as the user is already authenticated via UI session

        keycloakPort.changeUserPassword(providerUserId, request.getNewPassword());
        log.info("User {} successfully changed their password", email);
    }
}
