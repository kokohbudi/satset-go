package com.omnip.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk inisialisasi data default saat aplikasi pertama kali dijalankan.
 * Service ini akan membuat menu dan role default yang diperlukan untuk sistem.
 */
@Service
@Slf4j
public class DataInitializationService implements ApplicationRunner {

    private final MenuService menuService;
    private final RoleService roleService;

    public DataInitializationService(MenuService menuService, RoleService roleService) {
        this.menuService = menuService;
        this.roleService = roleService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting data initialization...");
        
        try {
            // Initialize default menus
            menuService.initializeDefaultMenus();
            
            // Initialize default roles
            roleService.initializeDefaultRoles();
            
            // Setup default role-menu assignments
            setupDefaultRoleMenuAssignments();
            
            log.info("Data initialization completed successfully");
            
        } catch (Exception e) {
            log.error("Error during data initialization", e);
            throw e;
        }
    }

    /**
     * Setup default assignment menu ke role.
     */
    private void setupDefaultRoleMenuAssignments() {
        log.info("Setting up default role-menu assignments");
        
        try {
            // Setup menu untuk omnip-admin (full access)
            setupOmnipAdminMenus();
            
            // Setup menu untuk omnip-operator
            setupOmnipOperatorMenus();
            
            // Setup menu untuk omnip-store-admin
            setupStoreAdminMenus();
            
            // Setup menu untuk omnip-store-operator
            setupStoreOperatorMenus();
            
        } catch (Exception e) {
            log.error("Error setting up default role-menu assignments", e);
        }
    }

    private void setupOmnipAdminMenus() {
        var adminRole = roleService.findByRoleCode("omnip-admin");
        if (adminRole != null && (adminRole.getMenus() == null || adminRole.getMenus().isEmpty())) {
            var menuIds = java.util.List.of(
                getMenuIdByCode("DASHBOARD"),
                getMenuIdByCode("USER_MANAGEMENT"),
                getMenuIdByCode("USER_LIST"),
                getMenuIdByCode("USER_CREATE"),
                getMenuIdByCode("ROLE_MANAGEMENT"),
                getMenuIdByCode("VOUCHER_MANAGEMENT"),
                getMenuIdByCode("TRANSACTION_MANAGEMENT"),
                getMenuIdByCode("PRICE_MANAGEMENT"),
                getMenuIdByCode("CUSTOMER_MANAGEMENT")
            ).stream().filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList());
            
            if (!menuIds.isEmpty()) {
                roleService.setMenusForRole(adminRole.getId(), menuIds);
                log.info("Setup menus for omnip-admin: {} menus assigned", menuIds.size());
            }
        }
    }

    private void setupOmnipOperatorMenus() {
        var operatorRole = roleService.findByRoleCode("omnip-operator");
        if (operatorRole != null && (operatorRole.getMenus() == null || operatorRole.getMenus().isEmpty())) {
            var menuIds = java.util.List.of(
                getMenuIdByCode("DASHBOARD"),
                getMenuIdByCode("VOUCHER_MANAGEMENT"),
                getMenuIdByCode("TRANSACTION_MANAGEMENT"),
                getMenuIdByCode("PRICE_MANAGEMENT"),
                getMenuIdByCode("CUSTOMER_MANAGEMENT")
            ).stream().filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList());
            
            if (!menuIds.isEmpty()) {
                roleService.setMenusForRole(operatorRole.getId(), menuIds);
                log.info("Setup menus for omnip-operator: {} menus assigned", menuIds.size());
            }
        }
    }

    private void setupStoreAdminMenus() {
        var storeAdminRole = roleService.findByRoleCode("omnip-store-admin");
        if (storeAdminRole != null && (storeAdminRole.getMenus() == null || storeAdminRole.getMenus().isEmpty())) {
            var menuIds = java.util.List.of(
                getMenuIdByCode("DASHBOARD"),
                getMenuIdByCode("USER_MANAGEMENT"),
                getMenuIdByCode("USER_LIST"),
                getMenuIdByCode("USER_CREATE"),
                getMenuIdByCode("VOUCHER_MANAGEMENT"),
                getMenuIdByCode("TRANSACTION_MANAGEMENT"),
                getMenuIdByCode("PRICE_MANAGEMENT"),
                getMenuIdByCode("CUSTOMER_MANAGEMENT")
            ).stream().filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList());
            
            if (!menuIds.isEmpty()) {
                roleService.setMenusForRole(storeAdminRole.getId(), menuIds);
                log.info("Setup menus for omnip-store-admin: {} menus assigned", menuIds.size());
            }
        }
    }

    private void setupStoreOperatorMenus() {
        var storeOperatorRole = roleService.findByRoleCode("omnip-store-operator");
        if (storeOperatorRole != null && (storeOperatorRole.getMenus() == null || storeOperatorRole.getMenus().isEmpty())) {
            var menuIds = java.util.List.of(
                getMenuIdByCode("DASHBOARD"),
                getMenuIdByCode("TRANSACTION_MANAGEMENT"),
                getMenuIdByCode("CUSTOMER_MANAGEMENT")
            ).stream().filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList());
            
            if (!menuIds.isEmpty()) {
                roleService.setMenusForRole(storeOperatorRole.getId(), menuIds);
                log.info("Setup menus for omnip-store-operator: {} menus assigned", menuIds.size());
            }
        }
    }

    private java.util.UUID getMenuIdByCode(String menuCode) {
        var menu = menuService.findByMenuCode(menuCode);
        return menu != null ? menu.getId() : null;
    }
}
