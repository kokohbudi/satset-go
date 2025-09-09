package com.omnip.services;

import com.omnip.entities.Menus;
import com.omnip.repositories.MenusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service untuk mengelola menu dan permission dalam aplikasi.
 * Service ini menyediakan operasi CRUD untuk menu dan method untuk mengatur hierarki menu.
 */
@Service
@Slf4j
@Transactional
public class MenuService {

    private final MenusRepository menusRepository;

    public MenuService(MenusRepository menusRepository) {
        this.menusRepository = menusRepository;
    }

    /**
     * Membuat menu baru.
     *
     * @param menu Objek menu yang akan dibuat
     * @return Menu yang telah disimpan
     */
    public Menus createMenu(Menus menu) {
        log.info("Creating new menu with code: {}", menu.getMenuCode());
        
        // Validasi menu code tidak duplikat
        if (menusRepository.findByMenuCode(menu.getMenuCode()) != null) {
            throw new IllegalArgumentException("Menu code already exists: " + menu.getMenuCode());
        }
        
        return menusRepository.save(menu);
    }

    /**
     * Mengupdate menu yang sudah ada.
     *
     * @param menuId ID menu yang akan diupdate
     * @param updatedMenu Data menu yang baru
     * @return Menu yang telah diupdate
     */
    public Menus updateMenu(UUID menuId, Menus updatedMenu) {
        log.info("Updating menu with ID: {}", menuId);
        
        Optional<Menus> existingMenuOpt = menusRepository.findById(menuId);
        if (existingMenuOpt.isEmpty()) {
            throw new IllegalArgumentException("Menu not found with ID: " + menuId);
        }
        
        Menus existingMenu = existingMenuOpt.get();
        
        // Update fields
        existingMenu.setMenuName(updatedMenu.getMenuName());
        existingMenu.setDescription(updatedMenu.getDescription());
        existingMenu.setUrl(updatedMenu.getUrl());
        existingMenu.setIcon(updatedMenu.getIcon());
        existingMenu.setSortOrder(updatedMenu.getSortOrder());
        existingMenu.setParentMenu(updatedMenu.getParentMenu());
        existingMenu.setActive(updatedMenu.isActive());
        
        return menusRepository.save(existingMenu);
    }

    /**
     * Menghapus menu (soft delete).
     *
     * @param menuId ID menu yang akan dihapus
     */
    public void deleteMenu(UUID menuId) {
        log.info("Deleting menu with ID: {}", menuId);
        
        Optional<Menus> menuOpt = menusRepository.findById(menuId);
        if (menuOpt.isEmpty()) {
            throw new IllegalArgumentException("Menu not found with ID: " + menuId);
        }
        
        Menus menu = menuOpt.get();
        menu.setDeleted(true);
        menu.setActive(false);
        
        menusRepository.save(menu);
    }

    /**
     * Mendapatkan semua menu yang aktif.
     *
     * @return List menu yang aktif
     */
    @Transactional(readOnly = true)
    public List<Menus> getAllActiveMenus() {
        return menusRepository.findAllActiveMenus();
    }

    /**
     * Mendapatkan semua menu parent (menu utama).
     *
     * @return List menu parent yang aktif
     */
    @Transactional(readOnly = true)
    public List<Menus> getAllParentMenus() {
        return menusRepository.findAllParentMenus();
    }

    /**
     * Mendapatkan submenu berdasarkan parent menu ID.
     *
     * @param parentMenuId ID menu parent
     * @return List submenu yang aktif
     */
    @Transactional(readOnly = true)
    public List<Menus> getSubMenusByParentId(UUID parentMenuId) {
        return menusRepository.findSubMenusByParentId(parentMenuId);
    }

    /**
     * Mencari menu berdasarkan menu code.
     *
     * @param menuCode Kode menu yang dicari
     * @return Menu jika ditemukan, null jika tidak ditemukan
     */
    @Transactional(readOnly = true)
    public Menus findByMenuCode(String menuCode) {
        return menusRepository.findByMenuCode(menuCode);
    }

    /**
     * Mencari menu berdasarkan ID.
     *
     * @param menuId ID menu yang dicari
     * @return Optional menu
     */
    @Transactional(readOnly = true)
    public Optional<Menus> findById(UUID menuId) {
        return menusRepository.findById(menuId);
    }

    /**
     * Mencari menu berdasarkan URL.
     *
     * @param url URL menu yang dicari
     * @return Menu jika ditemukan, null jika tidak ditemukan
     */
    @Transactional(readOnly = true)
    public Menus findByUrl(String url) {
        return menusRepository.findByUrlAndActiveTrue(url);
    }

    /**
     * Inisialisasi menu default untuk aplikasi.
     * Method ini akan dipanggil saat aplikasi pertama kali dijalankan.
     */
    public void initializeDefaultMenus() {
        log.info("Initializing default menus");
        
        // Menu utama untuk Dashboard
        createMenuIfNotExists("DASHBOARD", "Dashboard", "Dashboard utama aplikasi", "/dashboard", "fas fa-tachometer-alt", 1, null);
        
        // Menu untuk User Management
        createMenuIfNotExists("USER_MANAGEMENT", "Manajemen User", "Mengelola user dan role", "/users", "fas fa-users", 2, null);
        createMenuIfNotExists("USER_LIST", "Daftar User", "Melihat daftar user", "/users/list", "fas fa-list", 1, "USER_MANAGEMENT");
        createMenuIfNotExists("USER_CREATE", "Tambah User", "Menambah user baru", "/users/create", "fas fa-plus", 2, "USER_MANAGEMENT");
        createMenuIfNotExists("ROLE_MANAGEMENT", "Manajemen Role", "Mengelola role dan permission", "/roles", "fas fa-user-tag", 3, "USER_MANAGEMENT");
        
        // Menu untuk Voucher Management
        createMenuIfNotExists("VOUCHER_MANAGEMENT", "Manajemen Voucher", "Mengelola voucher pulsa", "/vouchers", "fas fa-ticket-alt", 3, null);
        
        // Menu untuk Transaction Management
        createMenuIfNotExists("TRANSACTION_MANAGEMENT", "Manajemen Transaksi", "Mengelola transaksi", "/transactions", "fas fa-exchange-alt", 4, null);
        
        // Menu untuk Price Management
        createMenuIfNotExists("PRICE_MANAGEMENT", "Manajemen Harga", "Mengelola harga beli dan jual", "/prices", "fas fa-dollar-sign", 5, null);
        
        // Menu untuk Customer Management
        createMenuIfNotExists("CUSTOMER_MANAGEMENT", "Manajemen Pelanggan", "Mengelola data pelanggan", "/customers", "fas fa-address-book", 6, null);
        
        log.info("Default menus initialization completed");
    }

    private void createMenuIfNotExists(String menuCode, String menuName, String description, String url, String icon, Integer sortOrder, String parentMenuCode) {
        if (menusRepository.findByMenuCode(menuCode) == null) {
            Menus menu = new Menus();
            menu.setMenuCode(menuCode);
            menu.setMenuName(menuName);
            menu.setDescription(description);
            menu.setUrl(url);
            menu.setIcon(icon);
            menu.setSortOrder(sortOrder);
            menu.setActive(true);
            
            if (parentMenuCode != null) {
                Menus parentMenu = menusRepository.findByMenuCode(parentMenuCode);
                menu.setParentMenu(parentMenu);
            }
            
            menusRepository.save(menu);
            log.info("Created menu: {}", menuCode);
        }
    }
}
