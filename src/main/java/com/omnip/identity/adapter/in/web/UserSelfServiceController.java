package com.omnip.identity.adapter.in.web;

import com.omnip.identity.adapter.in.web.dto.ChangeMyPasswordRequestDTO;
import com.omnip.identity.domain.port.in.ManageMyProfileUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserSelfServiceController {

    private final ManageMyProfileUseCase manageMyProfileUseCase;

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangeMyPasswordRequestDTO requestDTO) {

        String providerUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        try {
            manageMyProfileUseCase.changeMyPassword(providerUserId, email, requestDTO);
            return ResponseEntity.ok(Map.of("message", "Password berhasil diubah"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
