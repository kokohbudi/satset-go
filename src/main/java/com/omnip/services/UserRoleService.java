package com.omnip.services;

import com.omnip.entities.Menus;
import com.omnip.entities.Roles;
import com.omnip.entities.UserRoles;
import com.omnip.entities.Users;
import com.omnip.repositories.RolesRepository;
import com.omnip.repositories.UserRolesRepository;
import com.omnip.repositories.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service untuk mengelola assignment role ke user.
 * Service ini menyediakan operasi untuk assign/unassign role ke user dan mendapatkan menu yang dapat diakses user.
 */
@Service
@Slf4j
@Transactional
public class UserRoleService {

    private final UserRolesRepository userRolesRepository;
    private final UsersRepository usersRepository;
    private final RolesRepository rolesRepository;

    public UserRoleService(UserRolesRepository userRolesRepository, 
                          UsersRepository usersRepository, 
                          RolesRepository rolesRepository) {
        this.userRolesRepository = userRolesRepository;
        this.usersRepository = usersRepository;
        this.rolesRepository = rolesRepository;
    }

    /**
     * Assign role ke user.
     *
     * @param userId ID user yang akan di-assign role
     * @param roleId ID role yang akan di-assign
     * @param assignedByUserId ID user yang melakukan assignment
     * @param notes Catatan untuk assignment ini
     * @return UserRoles yang telah dibuat
     */
    public UserRoles assignRoleToUser(UUID userId, UUID roleId, UUID assignedByUserId, String notes) {
        log.info("Assigning role {} to user {} by user {}", roleId, userId, assignedByUserId);
        
        // Validasi user dan role exists
        Optional<Users> userOpt = usersRepository.findById(userId);
        Optional<Roles> roleOpt = rolesRepository.findById(roleId);
        Optional<Users> assignedByUserOpt = usersRepository.findById(assignedByUserId);
        
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        if (roleOpt.isEmpty()) {
            throw new IllegalArgumentException("Role not found with ID: " + roleId);
        }
        if (assignedByUserOpt.isEmpty()) {
            throw new IllegalArgumentException("Assigned by user not found with ID: " + assignedByUserId);
        }
        
        // Check if assignment already exists
        UserRoles existingAssignment = userRolesRepository.findByUserIdAndRoleId(userId, roleId);
        if (existingAssignment != null) {
            if (existingAssignment.isActive()) {
                throw new IllegalArgumentException("User already has this role assigned");
            } else {
                // Reactivate existing assignment
                existingAssignment.setActive(true);
                existingAssignment.setDeleted(false);
                existingAssignment.setNotes(notes);
                existingAssignment.setAssignedBy(assignedByUserOpt.get());
                return userRolesRepository.save(existingAssignment);
            }
        }
        
        // Create new assignment
        UserRoles userRole = new UserRoles();
        userRole.setUser(userOpt.get());
        userRole.setRole(roleOpt.get());
        userRole.setAssignedBy(assignedByUserOpt.get());
        userRole.setNotes(notes);
        userRole.setActive(true);
        
        return userRolesRepository.save(userRole);
    }

    /**
     * Unassign role dari user.
     *
     * @param userId ID user
     * @param roleId ID role yang akan di-unassign
     */
    public void unassignRoleFromUser(UUID userId, UUID roleId) {
        log.info("Unassigning role {} from user {}", roleId, userId);
        
        UserRoles userRole = userRolesRepository.findByUserIdAndRoleId(userId, roleId);
        if (userRole == null) {
            throw new IllegalArgumentException("User role assignment not found");
        }
        
        userRole.setActive(false);
        userRole.setDeleted(true);
        
        userRolesRepository.save(userRole);
    }

    /**
     * Mendapatkan semua role yang dimiliki oleh user.
     *
     * @param userId ID user
     * @return List role yang dimiliki user
     */
    @Transactional(readOnly = true)
    public List<Roles> getUserRoles(UUID userId) {
        List<UserRoles> userRoles = userRolesRepository.findActiveRolesByUserId(userId);
        return userRoles.stream()
                .map(UserRoles::getRole)
                .collect(Collectors.toList());
    }

    /**
     * Mendapatkan semua role yang dimiliki oleh user berdasarkan provider user ID.
     *
     * @param providerUserId Provider user ID dari Keycloak
     * @return List role yang dimiliki user
     */
    @Transactional(readOnly = true)
    public List<Roles> getUserRolesByProviderUserId(String providerUserId) {
        List<UserRoles> userRoles = userRolesRepository.findActiveRolesByProviderUserId(providerUserId);
        return userRoles.stream()
                .map(UserRoles::getRole)
                .collect(Collectors.toList());
    }

    /**
     * Mendapatkan semua menu yang dapat diakses oleh user berdasarkan role-role yang dimilikinya.
     *
     * @param userId ID user
     * @return List menu yang dapat diakses user
     */
    @Transactional(readOnly = true)
    public List<Menus> getUserAccessibleMenus(UUID userId) {
        return userRolesRepository.findAccessibleMenusByUserId(userId);
    }

    /**
     * Mendapatkan semua menu yang dapat diakses oleh user berdasarkan provider user ID.
     *
     * @param providerUserId Provider user ID dari Keycloak
     * @return List menu yang dapat diakses user
     */
    @Transactional(readOnly = true)
    public List<Menus> getUserAccessibleMenusByProviderUserId(String providerUserId) {
        return userRolesRepository.findAccessibleMenusByProviderUserId(providerUserId);
    }

    /**
     * Mendapatkan semua user yang memiliki role tertentu.
     *
     * @param roleId ID role
     * @return List user yang memiliki role tersebut
     */
    @Transactional(readOnly = true)
    public List<Users> getUsersByRole(UUID roleId) {
        List<UserRoles> userRoles = userRolesRepository.findActiveUsersByRoleId(roleId);
        return userRoles.stream()
                .map(UserRoles::getUser)
                .collect(Collectors.toList());
    }

    /**
     * Check apakah user memiliki role tertentu.
     *
     * @param userId ID user
     * @param roleCode Kode role yang dicek
     * @return true jika user memiliki role tersebut, false jika tidak
     */
    @Transactional(readOnly = true)
    public boolean hasRole(UUID userId, String roleCode) {
        List<Roles> userRoles = getUserRoles(userId);
        return userRoles.stream()
                .anyMatch(role -> role.getRoleCode().equals(roleCode));
    }

    /**
     * Check apakah user memiliki role tertentu berdasarkan provider user ID.
     *
     * @param providerUserId Provider user ID dari Keycloak
     * @param roleCode Kode role yang dicek
     * @return true jika user memiliki role tersebut, false jika tidak
     */
    @Transactional(readOnly = true)
    public boolean hasRoleByProviderUserId(String providerUserId, String roleCode) {
        List<Roles> userRoles = getUserRolesByProviderUserId(providerUserId);
        return userRoles.stream()
                .anyMatch(role -> role.getRoleCode().equals(roleCode));
    }

    /**
     * Check apakah user memiliki akses ke menu tertentu.
     *
     * @param userId ID user
     * @param menuCode Kode menu yang dicek
     * @return true jika user memiliki akses ke menu tersebut, false jika tidak
     */
    @Transactional(readOnly = true)
    public boolean hasMenuAccess(UUID userId, String menuCode) {
        List<Menus> accessibleMenus = getUserAccessibleMenus(userId);
        return accessibleMenus.stream()
                .anyMatch(menu -> menu.getMenuCode().equals(menuCode));
    }

    /**
     * Check apakah user memiliki akses ke menu tertentu berdasarkan provider user ID.
     *
     * @param providerUserId Provider user ID dari Keycloak
     * @param menuCode Kode menu yang dicek
     * @return true jika user memiliki akses ke menu tersebut, false jika tidak
     */
    @Transactional(readOnly = true)
    public boolean hasMenuAccessByProviderUserId(String providerUserId, String menuCode) {
        List<Menus> accessibleMenus = getUserAccessibleMenusByProviderUserId(providerUserId);
        return accessibleMenus.stream()
                .anyMatch(menu -> menu.getMenuCode().equals(menuCode));
    }

    /**
     * Assign multiple roles ke user sekaligus.
     *
     * @param userId ID user yang akan di-assign roles
     * @param roleIds List ID role yang akan di-assign
     * @param assignedByUserId ID user yang melakukan assignment
     * @param notes Catatan untuk assignment ini
     */
    public void assignMultipleRolesToUser(UUID userId, List<UUID> roleIds, UUID assignedByUserId, String notes) {
        log.info("Assigning multiple roles {} to user {} by user {}", roleIds, userId, assignedByUserId);
        
        for (UUID roleId : roleIds) {
            try {
                assignRoleToUser(userId, roleId, assignedByUserId, notes);
            } catch (IllegalArgumentException e) {
                log.warn("Failed to assign role {} to user {}: {}", roleId, userId, e.getMessage());
            }
        }
    }

    /**
     * Replace semua role user dengan role baru.
     *
     * @param userId ID user
     * @param newRoleIds List ID role baru
     * @param assignedByUserId ID user yang melakukan assignment
     * @param notes Catatan untuk assignment ini
     */
    public void replaceUserRoles(UUID userId, List<UUID> newRoleIds, UUID assignedByUserId, String notes) {
        log.info("Replacing roles for user {} with roles {} by user {}", userId, newRoleIds, assignedByUserId);
        
        // Get current roles
        List<UserRoles> currentUserRoles = userRolesRepository.findActiveRolesByUserId(userId);
        
        // Deactivate all current roles
        for (UserRoles userRole : currentUserRoles) {
            userRole.setActive(false);
            userRole.setDeleted(true);
        }
        userRolesRepository.saveAll(currentUserRoles);
        
        // Assign new roles
        assignMultipleRolesToUser(userId, newRoleIds, assignedByUserId, notes);
    }
}
