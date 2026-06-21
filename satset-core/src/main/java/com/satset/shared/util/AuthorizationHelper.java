package com.satset.shared.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper untuk custom authorization expressions di @PreAuthorize.
 * Digunakan dengan syntax: @authz.methodName(args)
 */
@Component("authz")
public class AuthorizationHelper {

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Cek apakah target user ID BUKAN user yang sedang login.
     * Digunakan untuk mencegah user melakukan aksi pada dirinya sendiri.
     *
     * @param targetUserId ID user yang menjadi target aksi
     * @return true jika target BUKAN current user
     */
    public boolean targetIsNotCurrentUser(String targetUserId) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || targetUserId == null) {
            return true; // Default allow jika tidak bisa determine
        }
        return !currentUserId.equals(targetUserId);
    }

    private String getCurrentUserId() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            return oidcUser.getSubject();
        } else if (principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            return jwt.getClaim("sub");
        }
        return null;
    }
}
