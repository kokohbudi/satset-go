package com.satset.shared.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
     * Cek apakah user memiliki group dengan prefix tertentu.
     * Groups di token biasanya dalam format: GROUP_/path/to/group
     *
     * @param prefix Group path prefix (e.g., "/backoffice/")
     * @return true jika user punya minimal satu group dengan prefix tersebut
     */
    public boolean hasGroupPrefix(String prefix) {
        Authentication auth = getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.startsWith("GROUP_" + prefix));
    }

    /**
     * Cek apakah user adalah member dari group tertentu (exact match).
     *
     * @param groupPath Full group path (e.g., "/backoffice/bo-admin")
     * @return true jika user adalah member dari group tersebut
     */
    public boolean isMemberOf(String groupPath) {
        Authentication auth = getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        String expectedAuthority = "GROUP_" + groupPath;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(expectedAuthority));
    }

    /**
     * Mendapatkan current user providerUserId (subject claim).
     *
     * @return providerUserId atau null jika tidak authenticated
     */
    public String getCurrentUserId() {
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
}
