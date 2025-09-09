package com.omnip.repositories;

import com.omnip.entities.Roles;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface untuk entity Roles.
 * Menyediakan operasi CRUD dasar dan method custom untuk mengakses data role.
 */
@Repository
public interface RolesRepository extends JpaRepository<Roles, UUID> {

    /**
     * Mencari role berdasarkan role code.
     * 
     * @param roleCode Kode role yang dicari
     * @return Objek Roles jika ditemukan, null jika tidak ditemukan
     */
    @Cacheable(value = "roles", key = "#roleCode", cacheManager = "fastCacheManager")
    Roles findByRoleCode(String roleCode);

    /**
     * Mencari semua role yang aktif dan tidak dihapus.
     * 
     * @return List role yang aktif
     */
    @Query("SELECT r FROM Roles r WHERE r.active = true AND r.deleted = false ORDER BY r.roleName ASC")
    List<Roles> findAllActiveRoles();

    /**
     * Mencari role berdasarkan list role codes.
     * 
     * @param roleCodes List kode role yang dicari
     * @return List role yang ditemukan
     */
    @Query("SELECT r FROM Roles r WHERE r.roleCode IN :roleCodes AND r.active = true AND r.deleted = false")
    List<Roles> findByRoleCodeIn(@Param("roleCodes") List<String> roleCodes);

    /**
     * Mencari role dengan menu yang terkait.
     * 
     * @param roleCode Kode role yang dicari
     * @return Objek Roles dengan menu yang terkait
     */
    @Query("SELECT r FROM Roles r LEFT JOIN FETCH r.menus WHERE r.roleCode = :roleCode AND r.active = true AND r.deleted = false")
    Roles findByRoleCodeWithMenus(@Param("roleCode") String roleCode);
}
