package com.satset.shared.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationHelperTest {

    private final AuthorizationHelper helper = new AuthorizationHelper();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== targetIsNotCurrentUser ====================

    @Test
    void targetIsNotCurrentUser_DifferentUser_ReturnsTrue() {
        setJwtAuth("user-current");

        assertTrue(helper.targetIsNotCurrentUser("user-other"));
    }

    @Test
    void targetIsNotCurrentUser_SameUser_ReturnsFalse() {
        setJwtAuth("user-current");

        assertFalse(helper.targetIsNotCurrentUser("user-current"));
    }

    @Test
    void targetIsNotCurrentUser_NullCurrentUser_ReturnsTrue() {
        setAuthWithAuthorities("ROLE_USER"); // no JWT, getCurrentUserId returns null

        assertTrue(helper.targetIsNotCurrentUser("user-xyz"));
    }

    @Test
    void targetIsNotCurrentUser_NullTargetId_ReturnsTrue() {
        setJwtAuth("user-current");

        assertTrue(helper.targetIsNotCurrentUser(null));
    }

    // ==================== Helpers ====================

    private void setAuthWithAuthorities(String... authorities) {
        var grantedAuthorities = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        var auth = new UsernamePasswordAuthenticationToken("user", "creds", grantedAuthorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setJwtAuth(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(claims -> claims.putAll(Map.of("sub", subject, "email", "test@mail.com")))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of()));
    }
}
