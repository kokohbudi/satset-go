package com.omnip.services;

import com.omnip.entities.Menus;
import com.omnip.entities.Roles;
import com.omnip.repositories.MenusRepository;
import com.omnip.repositories.RolesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service untuk mengelola role dan assignment menu ke role.
 * Service ini menyediakan operasi CRUD untuk role dan method untuk mengatur menu yang dapat diakses oleh role.
 */
@Service
@Slf4j
@Transactional
public class RoleService {

    private final RolesRepository rolesRepository;
    private final MenusRepository menusRepository;

    public RoleService(RolesRepository rolesRepository, MenusRepository menusRepository) {
        this.rolesRepository = rolesRepository;
        this.menusRepository = menusRepository;
    }

    /**
     * Membuat role baru.
     *
     * @param role Objek role yang akan dibuat
     * @return Role yang telah disimpan
     */
    public Roles createRole(Roles role) {
        log.info("Creating new role with code: {}", role.getRoleCode());
        
        // Validasi role code tidak duplikat
        if (rolesRepository.findByRoleCode(role.getRoleCode()) != null) {
            throw new IllegalArgumentException("Role code already exists: " + role.getRoleCode());
        }
        
        return rolesRepository.save(role);
    }

    /**
     * Mengupdate role yang sudah ada.
     *
     * @param roleId ID role yang akan diupdate
     * @param updatedRole Data role yang baru
     * @return Role yang telah diupdate
     */
    public Roles updateRole(UUID roleId, Roles updatedRole) {
        log.info("Updating role with ID: {}", roleId);
        
        Optional<Roles> existingRoleOpt = rolesRepository.findById(roleId);
        if (existingRoleOpt.isEmpty()) {
            throw new IllegalArgumentException("Role not found with ID: " + roleId);
        }
        
        Roles existingRole = existingRoleOpt.get();
        
        // Update fields
        existingRole.setRoleName(updatedRole.getRoleName());
        existingRole.setDescription(updatedRole.getDescription());
        existingRole.setActive(updatedRole.isActive());
        
        return rolesRepository.save(existingRole);
    }

    /**
     * Menghapus role (soft delete).
     *
     * @param roleId ID role yang akan dihapus
     */
    public void deleteRole(UUID roleId) {
        log.info("Deleting role with ID: {}", roleId);
        
        Optional<Roles> roleOpt = rolesRepository.findById(roleId);
        if (roleOpt.isEmpty()) {
            throw new IllegalArgumentException("Role not found with ID: " + roleId);
        }
        
        Roles role = roleOpt.get();
        role.setDeleted(true);
        role.setActive(false);
        
        rolesRepository.save(role);
    }

    /**
     * Mendapatkan semua role yang aktif.
     *
     * @return List role yang aktif
     */
    @Transactional(readOnly = true)
    public List<Roles> getAllActiveRoles() {
        return rolesRepository.findAllActiveRoles();
    }

    /**
     * Mencari role berdasarkan role code.
     *
     * @param roleCode Kode role yang dicari
     * @return Role jika ditemukan, null jika tidak ditemukan
     */
    @Transactional(readOnly = true)
    public Roles findByRoleCode(String roleCode) {
        return rolesRepository.findByRoleCode(roleCode);
    }

    /**
     * Mencari role berdasarkan ID.
     *
     * @param roleId ID role yang dicari
     * @return Optional role
     */
    @Transactional(readOnly = true)
    public Optional<Roles> findById(UUID roleId) {
        return rolesRepository.findById(roleId);
    }

    /**
     * Mencari role dengan menu yang terkait.
     *
     * @param roleCode Kode role yang dicari
     * @return Role dengan menu yang terkait
     */
    @Transactional(readOnly = true)
    public Roles findByRoleCodeWithMenus(String roleCode) {
        return rolesRepository.findByRoleCodeWithMenus(roleCode);
    }

    /**
     * Menambahkan menu ke role.
     *
     * @param roleId ID role
     * @param menuId ID menu yang akan ditambahkan
     */
    public void addMenuToRole(UUID roleId, UUID menuId) {
        log.info("Adding menu {} to role {}", menuId, roleId);
        
        Optional<Roles> roleOpt = rolesRepository.findById(roleId);
        Optional<Menus> menuOpt = menusRepository.findById(menuId);
        
        if (roleOpt.isEmpty()) {
            throw new IllegalArgumentException("Role not found with ID: " + roleId);
        }
        if (menuOpt.isEmpty()) {
            throw new IllegalArgumentException("Menu not found with ID: " + menuId);
        }
        
        Roles role = roleOpt.get();
        Menus menu = menuOpt.get();
        
        if (role.getMenus() == null) {
            role.setMenus(new ArrayList<>());
        }
        if (!role.getMenus().contains(menu)) {
            role.getMenus().add(menu);
            rolesRepository.save(role);
        }
    }

    /**
     * Menghapus menu dari role.
     *
     * @param roleId ID role
     * @param menuId ID menu yang akan dihapus
     */
    public void removeMenuFromRole(UUID roleId, UUID menuId) {
        log.info("Removing menu {} from role {}", menuId, roleId);
        
        Optional<Roles> roleOpt = rolesRepository.findById(roleId);
        Optional<Menus> menuOpt = menusRepository.findById(menuId);
        
        if (roleOpt.isEmpty()) {
            throw new IllegalArgumentException("Role not found with ID: " + roleId);
        }
        if (menuOpt.isEmpty()) {
            throw new IllegalArgumentException("Menu not found with ID: " + menuId);
        }
        
        Roles role = roleOpt.get();
        Menus menu = menuOpt.get();
        
        if (role.getMenus() != null) {
            role.getMenus().remove(menu);
        }
        rolesRepository.save(role);
    }

    /**
     * Mengatur menu untuk role (replace semua menu yang ada).
     *
     * @param roleId ID role
     * @param menuIds List ID menu yang akan di-assign ke role
     */
    public void setMenusForRole(UUID roleId, List<UUID> menuIds) {
        log.info("Setting menus for role {}: {}", roleId, menuIds);
        
        Optional<Roles> roleOpt = rolesRepository.findById(roleId);
        if (roleOpt.isEmpty()) {
            throw new IllegalArgumentException("Role not found with ID: " + roleId);
        }
        
        Roles role = roleOpt.get();
        
        // Clear existing menus
        if (role.getMenus() == null) {
            role.setMenus(new ArrayList<>());
        } else {
            role.getMenus().clear();
        }
        
        // Add new menus
        for (UUID menuId : menuIds) {
            Optional<Menus> menuOpt = menusRepository.findById(menuId);
            if (menuOpt.isPresent()) {
                role.getMenus().add(menuOpt.get());
            } else {
                log.warn("Menu not found with ID: {}", menuId);
            }
        }
        
        rolesRepository.save(role);
    }

    /**
     * Inisialisasi role default untuk aplikasi.
     * Method ini akan dipanggil saat aplikasi pertama kali dijalankan.
     */
    public void initializeDefaultRoles() {
        log.info("Initializing default roles");
        
        // Role omnip-admin
        createRoleIfNotExists("omnip-admin", "Omnip Admin", 
            "Administrator utama yang memiliki kuasa untuk mengatur menu apa saja yang dimiliki oleh omnip-operator");
        
        // Role omnip-operator
        createRoleIfNotExists("omnip-operator", "Omnip Operator", 
            "Operator yang memiliki kuasa untuk mengatur voucher, mengatur harga, mengatur transaksi, mengatur pelanggan dan operasi lainnya");
        
        // Role omnip-store-admin
        createRoleIfNotExists("omnip-store-admin", "Store Admin", 
            "Pemilik toko yang bisa meng-assign suatu role ke suatu user dibawahnya, dia bisa membuat user, mengatur harga, dan memberikan menu ke dirinya sendiri atau ke user dibawahnya");
        
        // Role omnip-store-operator
        createRoleIfNotExists("omnip-store-operator", "Store Operator", 
            "Operator toko yang bisa melakukan transaksi dan melihat transaksi");
        
        log.info("Default roles initialization completed");
    }

    private void createRoleIfNotExists(String roleCode, String roleName, String description) {
        if (rolesRepository.findByRoleCode(roleCode) == null) {
            Roles role = new Roles();
            role.setRoleCode(roleCode);
            role.setRoleName(roleName);
            role.setDescription(description);
            role.setActive(true);
            
            rolesRepository.save(role);
            log.info("Created role: {}", roleCode);
        }
    }
}
