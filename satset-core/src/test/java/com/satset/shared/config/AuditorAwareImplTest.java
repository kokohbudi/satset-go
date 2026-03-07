package com.satset.shared.config;

import com.satset.shared.dto.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditorAwareImplTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== no authentication ====================

    @Test
    void getCurrentAuditor_NoAuthentication_ReturnsSystem() {
        SecurityContextHolder.clearContext();
        UserDTO dto = new UserDTO();
        AuditorAwareImpl auditor = new AuditorAwareImpl(dto);

        Optional<String> result = auditor.getCurrentAuditor();

        assertEquals(Optional.of("SYSTEM"), result);
    }

    // ==================== with UserDTO ====================

    @Test
    void getCurrentAuditor_AuthenticatedWithUserDTO_ReturnsUsername() {
        setBasicAuth("alice");
        UserDTO dto = new UserDTO();
        dto.setUsername("alice");
        AuditorAwareImpl auditor = new AuditorAwareImpl(dto);

        Optional<String> result = auditor.getCurrentAuditor();

        assertEquals(Optional.of("alice"), result);
    }

    @Test
    void getCurrentAuditor_NullUserDTO_FallsBackToAuthName() {
        setBasicAuth("bob");
        AuditorAwareImpl auditor = new AuditorAwareImpl(null);

        Optional<String> result = auditor.getCurrentAuditor();

        // With null userDTO, falls back to authentication.getName()
        assertEquals(Optional.of("bob"), result);
    }

    // ==================== helpers ====================

    private void setBasicAuth(String principal) {
        var auth = new UsernamePasswordAuthenticationToken(principal, "creds", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
