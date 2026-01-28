package com.omnip.utils;

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
}
