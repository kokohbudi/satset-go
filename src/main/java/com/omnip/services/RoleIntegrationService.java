package com.omnip.services;

import com.omnip.entities.Roles;
import com.omnip.entities.Users;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service untuk integrasi role management dengan Keycloak.
 * Service ini membantu sinkronisasi dan validasi role antara Keycloak dan database aplikasi.
 */
@Service
@Slf4j
@Transactional
public class RoleIntegrationService {

    private final UserRoleService userRoleService;
    private final RoleService roleService;

    public RoleIntegrationService(UserRoleService userRoleService, RoleService roleService) {
        this.userRoleService = userRoleService;
        this.roleService = roleService;
    }

    /**
     * Mendapatkan role yang dimiliki user dari Keycloak token.
     *
     * @param authentication Authentication object dari Spring Security
     * @return Set role codes dari Keycloak
     */
    public Set<String> getKeycloakRoles(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            
            // Extract roles from Keycloak token
            // Roles biasanya ada di claim "realm_access" -> "roles" atau "resource_access" -> "client_id" -> "roles"
            var realmAccess = oidcUser.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");
                return roles.stream()
                        .filter(role -> role.startsWith("omnip-")) // Filter only omnip roles
                        .collect(Collectors.toSet());
            }
            
            // Alternative: check resource_access for client-specific roles
            var resourceAccess = oidcUser.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                @SuppressWarnings("unchecked")
                var clientAccess = (java.util.Map<String, Object>) resourceAccess.get("omnip-client");
                if (clientAccess != null && clientAccess.containsKey("roles")) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) clientAccess.get("roles");
                    return roles.stream()
                            .filter(role -> role.startsWith("omnip-"))
                            .collect(Collectors.toSet());
                }
            }
        }
        
        return Set.of();
    }

    /**
     * Sinkronisasi role user antara Keycloak dan database aplikasi.
     * Method ini akan memastikan role di database sesuai dengan role di Keycloak.
     *
     * @param user User yang akan disinkronisasi
     * @param keycloakRoles Role dari Keycloak
     */
    public void syncUserRoles(Users user, Set<String> keycloakRoles) {
        log.info("Syncing roles for user {} with Keycloak roles: {}", user.getEmail(), keycloakRoles);
        
        try {
            // Get current roles from database
            List<Roles> currentDbRoles = userRoleService.getUserRoles(user.getId());
            Set<String> currentDbRoleCodes = currentDbRoles.stream()
                    .map(Roles::getRoleCode)
                    .collect(Collectors.toSet());
            
            // Find roles to add (in Keycloak but not in DB)
            Set<String> rolesToAdd = keycloakRoles.stream()
                    .filter(roleCode -> !currentDbRoleCodes.contains(roleCode))
                    .collect(Collectors.toSet());
            
            // Find roles to remove (in DB but not in Keycloak)
            Set<String> rolesToRemove = currentDbRoleCodes.stream()
                    .filter(roleCode -> !keycloakRoles.contains(roleCode))
                    .collect(Collectors.toSet());
            
            // Add missing roles
            for (String roleCode : rolesToAdd) {
                Roles role = roleService.findByRoleCode(roleCode);
                if (role != null) {
                    userRoleService.assignRoleToUser(
                        user.getId(), 
                        role.getId(), 
                        user.getId(), // Self-assignment from Keycloak sync
                        "Auto-assigned from Keycloak sync"
                    );
                    log.info("Added role {} to user {}", roleCode, user.getEmail());
                }
            }
            
            // Remove extra roles (optional - might want to keep manual assignments)
            // Uncomment if you want strict sync
            /*
            for (String roleCode : rolesToRemove) {
                Roles role = roleService.findByRoleCode(roleCode);
                if (role != null) {
                    userRoleService.unassignRoleFromUser(user.getId(), role.getId());
                    log.info("Removed role {} from user {}", roleCode, user.getEmail());
                }
            }
            */
            
        } catch (Exception e) {
            log.error("Error syncing user roles for user {}", user.getEmail(), e);
        }
    }

    /**
     * Check apakah user memiliki permission untuk mengakses resource tertentu.
     * Method ini mengecek baik dari Keycloak roles maupun database roles.
     *
     * @param authentication Authentication object
     * @param requiredRole Role yang diperlukan
     * @return true jika user memiliki permission
     */
    public boolean hasPermission(Authentication authentication, String requiredRole) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        try {
            // Check Keycloak roles
            Set<String> keycloakRoles = getKeycloakRoles(authentication);
            if (keycloakRoles.contains(requiredRole)) {
                return true;
            }
            
            // Check database roles
            if (authentication.getPrincipal() instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
                String providerUserId = oidcUser.getSubject();
                return userRoleService.hasRoleByProviderUserId(providerUserId, requiredRole);
            }
            
        } catch (Exception e) {
            log.error("Error checking permission for role {}", requiredRole, e);
        }
        
        return false;
    }

    /**
     * Check apakah user memiliki akses ke menu tertentu.
     *
     * @param authentication Authentication object
     * @param menuCode Menu code yang akan dicek
     * @return true jika user memiliki akses
     */
    public boolean hasMenuAccess(Authentication authentication, String menuCode) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        try {
            if (authentication.getPrincipal() instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
                String providerUserId = oidcUser.getSubject();
                return userRoleService.hasMenuAccessByProviderUserId(providerUserId, menuCode);
            }
        } catch (Exception e) {
            log.error("Error checking menu access for menu {}", menuCode, e);
        }
        
        return false;
    }

    /**
     * Validasi apakah role code valid untuk sistem omnip.
     *
     * @param roleCode Role code yang akan divalidasi
     * @return true jika valid
     */
    public boolean isValidOmnipRole(String roleCode) {
        return roleCode != null && 
               roleCode.startsWith("omnip-") && 
               (roleCode.equals("omnip-admin") || 
                roleCode.equals("omnip-operator") || 
                roleCode.equals("omnip-store-admin") || 
                roleCode.equals("omnip-store-operator"));
    }

    /**
     * Get role hierarchy untuk user.
     * Method ini menentukan level akses user berdasarkan role yang dimiliki.
     *
     * @param userRoles List role yang dimiliki user
     * @return Level akses (1=admin, 2=operator, 3=store-admin, 4=store-operator)
     */
    public int getUserAccessLevel(List<Roles> userRoles) {
        Set<String> roleCodes = userRoles.stream()
                .map(Roles::getRoleCode)
                .collect(Collectors.toSet());
        
        if (roleCodes.contains("omnip-admin")) {
            return 1; // Highest access
        } else if (roleCodes.contains("omnip-operator")) {
            return 2;
        } else if (roleCodes.contains("omnip-store-admin")) {
            return 3;
        } else if (roleCodes.contains("omnip-store-operator")) {
            return 4; // Lowest access
        }
        
        return 999; // No access
    }
}
