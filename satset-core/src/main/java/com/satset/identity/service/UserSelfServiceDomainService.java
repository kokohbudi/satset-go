package com.satset.identity.service;

import com.satset.identity.model.ChangeMyPasswordRequest;
import com.satset.identity.client.KeycloakIdentityPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSelfServiceDomainService {

    private final KeycloakIdentityPort keycloakPort;

    public void changeMyPassword(String providerUserId, String email, ChangeMyPasswordRequest request) {
        if (!request.isPasswordMatch()) {
            throw new IllegalArgumentException("Password baru dan konfirmasi tidak cocok");
        }

        // Old password verification is skipped as the user is already authenticated via UI session

        keycloakPort.changeUserPassword(providerUserId, request.getNewPassword());
        log.info("User {} successfully changed their password", email);
    }
}
