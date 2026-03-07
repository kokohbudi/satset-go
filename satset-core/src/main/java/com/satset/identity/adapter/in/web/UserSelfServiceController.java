package com.satset.identity.adapter.in.web;

import com.satset.identity.domain.model.ChangeMyPasswordRequest;
import com.satset.identity.domain.port.in.ManageMyProfileUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            @Valid @RequestBody ChangeMyPasswordRequest request) {

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
            manageMyProfileUseCase.changeMyPassword(providerUserId, email, request);
            return ResponseEntity.ok(Map.of("message", "Password berhasil diubah"));
        } catch (IllegalArgumentException e) {
            log.warn("Password change validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Gagal mengubah password. Periksa kembali data Anda."));
        }
    }
}
