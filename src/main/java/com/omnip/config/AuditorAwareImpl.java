package com.omnip.config;

import com.omnip.dto.UserDTO;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    private final UserDTO userDTO;

    public AuditorAwareImpl(UserDTO userDTO) {
        this.userDTO = userDTO;
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("SYSTEM");
        }

        // Jika menggunakan OAuth2/JWT
        if (this.userDTO != null) {
            return Optional.of(this.userDTO.getUsername());
        }

        return Optional.of(authentication.getName());
    }
}