package com.omnip.identity.adapter.in.web;

import com.omnip.identity.adapter.in.web.dto.ChangeMyPasswordRequestDTO;
import com.omnip.identity.domain.port.in.ManageMyProfileUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserSelfServiceController {

    private final ManageMyProfileUseCase manageMyProfileUseCase;

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody ChangeMyPasswordRequestDTO requestDTO) {

        String providerUserId;
        String email;

        if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            providerUserId = oidcUser.getSubject();
            email = oidcUser.getEmail();
        } else if (principal instanceof Jwt jwt) {
            providerUserId = jwt.getSubject();
            email = jwt.getClaimAsString("email");
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "Unsupported authentication type"));
        }

        try {
            manageMyProfileUseCase.changeMyPassword(providerUserId, email, requestDTO);
            return ResponseEntity.ok(Map.of("message", "Password berhasil diubah"));
        } catch (IllegalArgumentException e) {
            log.warn("Password change validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Gagal mengubah password. Periksa kembali data Anda."));
        }
    }
}
