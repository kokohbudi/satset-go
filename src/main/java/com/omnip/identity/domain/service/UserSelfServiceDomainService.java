package com.omnip.identity.domain.service;

import com.omnip.identity.adapter.in.web.dto.ChangeMyPasswordRequestDTO;
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
    public void changeMyPassword(String providerUserId, String email, ChangeMyPasswordRequestDTO requestDTO) {
        if (!requestDTO.getNewPassword().equals(requestDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Password baru dan konfirmasi tidak cocok");
        }

        // Old password verification is skipped as the user is already authenticated via UI session

        keycloakPort.changeUserPassword(providerUserId, requestDTO.getNewPassword());
        log.info("User {} successfully changed their password", email);
    }
}
