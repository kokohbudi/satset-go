package com.omnip.utils;

import com.omnip.entities.Menus;
import com.omnip.entities.Users;
import com.omnip.services.UserRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class untuk filtering menu berdasarkan role user.
 * Digunakan di template Thymeleaf untuk menampilkan menu yang sesuai dengan role user.
 */
@Component("menuUtils")
@Slf4j
public class MenuUtils {

    private final UserRoleService userRoleService;

    public MenuUtils(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    /**
     * Mendapatkan menu yang dapat diakses oleh user berdasarkan authentication.
     *
     * @param authentication Authentication object dari Spring Security
     * @return List menu yang dapat diakses user
     */
    public List<Menus> getAccessibleMenus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }

        try {
            if (authentication.getPrincipal() instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
                String providerUserId = oidcUser.getSubject();
                return userRoleService.getUserAccessibleMenusByProviderUserId(providerUserId);
            }
        } catch (Exception e) {
            log.error("Error getting accessible menus for user", e);
        }

        return List.of();
    }

    /**
     * Mendapatkan parent menu yang dapat diakses oleh user.
     *
     * @param authentication Authentication object
     * @return List parent menu yang dapat diakses
     */
    public List<Menus> getAccessibleParentMenus(Authentication authentication) {
        List<Menus> allAccessibleMenus = getAccessibleMenus(authentication);
        return allAccessibleMenus.stream()
                .filter(menu -> menu.getParentMenu() == null)
                .collect(Collectors.toList());
    }

    /**
     * Mendapatkan submenu dari parent menu yang dapat diakses oleh user.
     *
     * @param authentication Authentication object
     * @param parentMenuCode Kode parent menu
     * @return List submenu yang dapat diakses
     */
    public List<Menus> getAccessibleSubMenus(Authentication authentication, String parentMenuCode) {
        List<Menus> allAccessibleMenus = getAccessibleMenus(authentication);
        return allAccessibleMenus.stream()
                .filter(menu -> menu.getParentMenu() != null && 
                               menu.getParentMenu().getMenuCode().equals(parentMenuCode))
                .collect(Collectors.toList());
    }

    /**
     * Check apakah user memiliki akses ke menu tertentu.
     *
     * @param authentication Authentication object
     * @param menuCode Kode menu yang dicek
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
     * Check apakah user memiliki role tertentu.
     *
     * @param authentication Authentication object
     * @param roleCode Kode role yang dicek
     * @return true jika user memiliki role
     */
    public boolean hasRole(Authentication authentication, String roleCode) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        try {
            if (authentication.getPrincipal() instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
                String providerUserId = oidcUser.getSubject();
                return userRoleService.hasRoleByProviderUserId(providerUserId, roleCode);
            }
        } catch (Exception e) {
            log.error("Error checking role {} for user", roleCode, e);
        }

        return false;
    }

    /**
     * Check apakah user adalah admin (omnip-admin atau omnip-store-admin).
     *
     * @param authentication Authentication object
     * @return true jika user adalah admin
     */
    public boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, "omnip-admin") || 
               hasRole(authentication, "omnip-store-admin");
    }

    /**
     * Check apakah user adalah operator (omnip-operator atau omnip-store-operator).
     *
     * @param authentication Authentication object
     * @return true jika user adalah operator
     */
    public boolean isOperator(Authentication authentication) {
        return hasRole(authentication, "omnip-operator") || 
               hasRole(authentication, "omnip-store-operator");
    }

    /**
     * Mendapatkan level akses user (1=admin, 2=operator, 3=store-admin, 4=store-operator).
     *
     * @param authentication Authentication object
     * @return Level akses user
     */
    public int getUserAccessLevel(Authentication authentication) {
        if (hasRole(authentication, "omnip-admin")) {
            return 1;
        } else if (hasRole(authentication, "omnip-operator")) {
            return 2;
        } else if (hasRole(authentication, "omnip-store-admin")) {
            return 3;
        } else if (hasRole(authentication, "omnip-store-operator")) {
            return 4;
        }
        return 999; // No access
    }

    /**
     * Mendapatkan display name untuk level akses user.
     *
     * @param authentication Authentication object
     * @return Display name level akses
     */
    public String getUserAccessLevelName(Authentication authentication) {
        int level = getUserAccessLevel(authentication);
        switch (level) {
            case 1: return "Super Admin";
            case 2: return "Operator";
            case 3: return "Store Admin";
            case 4: return "Store Operator";
            default: return "Guest";
        }
    }

    /**
     * Filter menu berdasarkan kategori dan akses user.
     *
     * @param authentication Authentication object
     * @param category Kategori menu (misalnya: "management", "transaction", "report")
     * @return List menu yang sesuai kategori dan dapat diakses user
     */
    public List<Menus> getMenusByCategory(Authentication authentication, String category) {
        List<Menus> accessibleMenus = getAccessibleMenus(authentication);
        
        // Simple categorization based on menu code patterns
        return accessibleMenus.stream()
                .filter(menu -> {
                    String menuCode = menu.getMenuCode().toLowerCase();
                    switch (category.toLowerCase()) {
                        case "management":
                            return menuCode.contains("management") || menuCode.contains("admin");
                        case "transaction":
                            return menuCode.contains("transaction") || menuCode.contains("voucher");
                        case "report":
                            return menuCode.contains("report") || menuCode.contains("dashboard");
                        case "setting":
                            return menuCode.contains("setting") || menuCode.contains("config");
                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());
    }
}
