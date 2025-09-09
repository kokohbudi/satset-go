package com.omnip.repositories;

import com.omnip.entities.Menus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface untuk entity Menus.
 * Menyediakan operasi CRUD dasar dan method custom untuk mengakses data menu.
 */
@Repository
public interface MenusRepository extends JpaRepository<Menus, UUID> {

    /**
     * Mencari menu berdasarkan menu code.
     * 
     * @param menuCode Kode menu yang dicari
     * @return Objek Menus jika ditemukan, null jika tidak ditemukan
     */
    @Cacheable(value = "menus", key = "#menuCode", cacheManager = "fastCacheManager")
    Menus findByMenuCode(String menuCode);

    /**
     * Mencari semua menu yang aktif dan tidak dihapus, diurutkan berdasarkan sortOrder.
     * 
     * @return List menu yang aktif
     */
    @Query("SELECT m FROM Menus m WHERE m.active = true AND m.deleted = false ORDER BY m.sortOrder ASC, m.menuName ASC")
    List<Menus> findAllActiveMenus();

    /**
     * Mencari menu parent (menu utama) yang aktif.
     * 
     * @return List menu parent yang aktif
     */
    @Query("SELECT m FROM Menus m WHERE m.parentMenu IS NULL AND m.active = true AND m.deleted = false ORDER BY m.sortOrder ASC, m.menuName ASC")
    List<Menus> findAllParentMenus();

    /**
     * Mencari submenu berdasarkan parent menu ID.
     * 
     * @param parentMenuId ID menu parent
     * @return List submenu yang aktif
     */
    @Query("SELECT m FROM Menus m WHERE m.parentMenu.id = :parentMenuId AND m.active = true AND m.deleted = false ORDER BY m.sortOrder ASC, m.menuName ASC")
    List<Menus> findSubMenusByParentId(@Param("parentMenuId") UUID parentMenuId);

    /**
     * Mencari menu berdasarkan URL.
     * 
     * @param url URL menu yang dicari
     * @return Objek Menus jika ditemukan, null jika tidak ditemukan
     */
    Menus findByUrlAndActiveTrue(String url);
}
