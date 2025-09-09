package com.omnip.repositories;

import com.omnip.entities.UserRoles;
import com.omnip.entities.Users;
import com.omnip.entities.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface untuk entity UserRoles.
 * Menyediakan operasi CRUD dasar dan method custom untuk mengakses data mapping user-role.
 */
@Repository
public interface UserRolesRepository extends JpaRepository<UserRoles, UUID> {

    /**
     * Mencari semua role yang dimiliki oleh user tertentu.
     * 
     * @param userId ID user
     * @return List UserRoles yang aktif untuk user tersebut
     */
    @Query("SELECT ur FROM UserRoles ur JOIN FETCH ur.role WHERE ur.user.id = :userId AND ur.active = true AND ur.deleted = false")
    List<UserRoles> findActiveRolesByUserId(@Param("userId") UUID userId);

    /**
     * Mencari semua role yang dimiliki oleh user berdasarkan provider user ID.
     * 
     * @param providerUserId Provider user ID dari Keycloak
     * @return List UserRoles yang aktif untuk user tersebut
     */
    @Query("SELECT ur FROM UserRoles ur JOIN FETCH ur.role WHERE ur.user.providerUserId = :providerUserId AND ur.active = true AND ur.deleted = false")
    List<UserRoles> findActiveRolesByProviderUserId(@Param("providerUserId") String providerUserId);

    /**
     * Mencari semua user yang memiliki role tertentu.
     * 
     * @param roleId ID role
     * @return List UserRoles yang aktif untuk role tersebut
     */
    @Query("SELECT ur FROM UserRoles ur JOIN FETCH ur.user WHERE ur.role.id = :roleId AND ur.active = true AND ur.deleted = false")
    List<UserRoles> findActiveUsersByRoleId(@Param("roleId") UUID roleId);

    /**
     * Mencari mapping user-role berdasarkan user ID dan role ID.
     * 
     * @param userId ID user
     * @param roleId ID role
     * @return UserRoles jika ditemukan, null jika tidak ditemukan
     */
    @Query("SELECT ur FROM UserRoles ur WHERE ur.user.id = :userId AND ur.role.id = :roleId AND ur.active = true AND ur.deleted = false")
    UserRoles findByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    /**
     * Mencari semua menu yang dapat diakses oleh user berdasarkan role-role yang dimilikinya.
     * 
     * @param userId ID user
     * @return List menu yang dapat diakses user
     */
    @Query("SELECT DISTINCT m FROM UserRoles ur JOIN ur.role r JOIN r.menus m WHERE ur.user.id = :userId AND ur.active = true AND ur.deleted = false AND r.active = true AND r.deleted = false AND m.active = true AND m.deleted = false ORDER BY m.sortOrder ASC, m.menuName ASC")
    List<com.omnip.entities.Menus> findAccessibleMenusByUserId(@Param("userId") UUID userId);

    /**
     * Mencari semua menu yang dapat diakses oleh user berdasarkan provider user ID.
     * 
     * @param providerUserId Provider user ID dari Keycloak
     * @return List menu yang dapat diakses user
     */
    @Query("SELECT DISTINCT m FROM UserRoles ur JOIN ur.role r JOIN r.menus m WHERE ur.user.providerUserId = :providerUserId AND ur.active = true AND ur.deleted = false AND r.active = true AND r.deleted = false AND m.active = true AND m.deleted = false ORDER BY m.sortOrder ASC, m.menuName ASC")
    List<com.omnip.entities.Menus> findAccessibleMenusByProviderUserId(@Param("providerUserId") String providerUserId);
}
