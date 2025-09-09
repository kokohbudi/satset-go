package com.omnip.controllers;

import com.omnip.dtos.MenuDTO;
import com.omnip.dtos.RoleDTO;
import com.omnip.dtos.UserDTO;
import com.omnip.dtos.UserRoleAssignmentDTO;
import com.omnip.entities.Menus;
import com.omnip.entities.Roles;
import com.omnip.entities.Users;
import com.omnip.services.MenuService;
import com.omnip.services.RoleService;
import com.omnip.services.UserRoleService;
import com.omnip.repositories.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller untuk mengelola role management dan menu assignment.
 * Menyediakan UI untuk admin mengelola role dan mengatur menu yang dapat diakses oleh setiap role.
 */
@Controller
@RequestMapping("/dashboard/admin/roles")
@Slf4j
public class RoleManagementController {

    private final RoleService roleService;
    private final MenuService menuService;
    private final UserRoleService userRoleService;
    private final UsersRepository usersRepository;

    public RoleManagementController(RoleService roleService, 
                                  MenuService menuService, 
                                  UserRoleService userRoleService,
                                  UsersRepository usersRepository) {
        this.roleService = roleService;
        this.menuService = menuService;
        this.userRoleService = userRoleService;
        this.usersRepository = usersRepository;
    }

    /**
     * Halaman utama role management - menampilkan daftar semua role.
     */
    @GetMapping
    @PreAuthorize("@userDTO.getRoles().contains('omnip-admin')")
    public String roleManagementPage(Model model, Authentication authentication) {
        log.info("Accessing role management page");
        
        // Check if user has permission to access role management
//        if (!hasRoleManagementPermission(authentication)) {
//            return "redirect:/dashboard?error=access_denied";
//        }
        
        List<Roles> roles = roleService.getAllActiveRoles();
        List<RoleDTO> roleDTOs = roles.stream()
                .map(this::convertToRoleDTO)
                .collect(Collectors.toList());
        
        model.addAttribute("roles", roleDTOs);
        model.addAttribute("pageTitle", "Manajemen Role");
        
        return "pages/admin/role-management";
    }

    /**
     * Halaman detail role - menampilkan detail role dan menu yang terkait.
     */
    @PreAuthorize("@userDTO.getRoles().contains('omnip-admin')")
    @GetMapping("/{roleId}")
    public String roleDetailPage(@PathVariable UUID roleId, Model model, Authentication authentication) {
        log.info("Accessing role detail page for role ID: {}", roleId);
        
//        if (!hasRoleManagementPermission(authentication)) {
//            return "redirect:/dashboard?error=access_denied";
//        }
        
        Roles role = roleService.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        RoleDTO roleDTO = convertToRoleDTO(role);
        List<Menus> allMenus = menuService.getAllActiveMenus();
        List<MenuDTO> allMenuDTOs = allMenus.stream()
                .map(this::convertToMenuDTO)
                .collect(Collectors.toList());
        
        model.addAttribute("role", roleDTO);
        model.addAttribute("allMenus", allMenuDTOs);
        model.addAttribute("pageTitle", "Detail Role: " + role.getRoleName());
        
        return "pages/admin/role-detail";
    }

    /**
     * Halaman untuk assign role ke user.
     */
    @GetMapping("/assign")
    @PreAuthorize("@userDTO.getRoles().contains('omnip-admin')")
    public String assignRolePage(Model model, Authentication authentication) {
        log.info("Accessing assign role page");
        

        
        List<Roles> roles = roleService.getAllActiveRoles();
        List<RoleDTO> roleDTOs = roles.stream()
                .map(this::convertToRoleDTO)
                .collect(Collectors.toList());
        
        model.addAttribute("roles", roleDTOs);
        model.addAttribute("pageTitle", "Assign Role ke User");
        
        return "pages/admin/assign-role";
    }

    /**
     * API endpoint untuk mendapatkan daftar role dalam format JSON.
     */
    @GetMapping("/api/list")
    @ResponseBody
    @PreAuthorize("@userDTO.getRoles().contains('omnip-admin')")
    public List<RoleDTO> getRolesList() {
        List<Roles> roles = roleService.getAllActiveRoles();
        return roles.stream()
                .map(this::convertToRoleDTO)
                .collect(Collectors.toList());
    }

    /**
     * API endpoint untuk mendapatkan detail role dalam format JSON.
     */
    @GetMapping("/api/{roleId}")
    @ResponseBody
    @PreAuthorize("@userDTO.getRoles().contains('omnip-admin')")
    public RoleDTO getRoleDetail(@PathVariable UUID roleId) {
        Roles role = roleService.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        return convertToRoleDTO(role);
    }

    /**
     * API endpoint untuk update menu assignment ke role.
     */
    @PostMapping("/api/{roleId}/menus")
    @ResponseBody
    @PreAuthorize("@userDTO.getRoles().contains('omnip-admin')")
    public RoleDTO updateRoleMenus(@PathVariable UUID roleId, 
                                  @RequestBody List<UUID> menuIds,
                                  Authentication authentication) {
        log.info("Updating menus for role {}: {}", roleId, menuIds);
        
//        if (!hasRoleManagementPermission(authentication)) {
//            throw new SecurityException("Access denied");
//        }
        
        roleService.setMenusForRole(roleId, menuIds);
        
        Roles updatedRole = roleService.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        RoleDTO roleDTO = convertToRoleDTO(updatedRole);
        roleDTO.setStatus("success");
        roleDTO.setMessage("Menu berhasil diupdate untuk role " + updatedRole.getRoleName());
        
        return roleDTO;
    }

    /**
     * API endpoint untuk assign role ke user.
     */
    @PostMapping("/api/assign")
    @ResponseBody
    @PreAuthorize("@userDTO.getRoles().contains('omnip-admin')")
    public UserRoleAssignmentDTO assignRoleToUser(@RequestBody UserRoleAssignmentDTO assignmentDTO,
                                                 Authentication authentication) {
        log.info("Assigning roles {} to user {}", assignmentDTO.getRoleIds(), assignmentDTO.getUserId());
        
        if (!hasRoleManagementPermission(authentication)) {
            throw new SecurityException("Access denied");
        }
        
        // Get current user ID for assignment tracking
        Users currentUser = getCurrentUser(authentication);
        
        try {
            userRoleService.assignMultipleRolesToUser(
                assignmentDTO.getUserId(), 
                assignmentDTO.getRoleIds(), 
                currentUser.getId(), 
                assignmentDTO.getNotes()
            );
            
            assignmentDTO.setStatus("success");
            assignmentDTO.setMessage("Role berhasil di-assign ke user");
            
        } catch (Exception e) {
            log.error("Error assigning roles to user", e);
            assignmentDTO.setStatus("error");
            assignmentDTO.setMessage("Gagal assign role: " + e.getMessage());
        }
        
        return assignmentDTO;
    }

    /**
     * API endpoint untuk unassign role dari user.
     */
    @DeleteMapping("/api/assign/{userId}/{roleId}")
    @ResponseBody
    @PreAuthorize("@userDTO.getRoles().contains('omnip-admin')")
    public UserRoleAssignmentDTO unassignRoleFromUser(@PathVariable UUID userId,
                                                     @PathVariable UUID roleId,
                                                     Authentication authentication) {
        log.info("Unassigning role {} from user {}", roleId, userId);
        
        if (!hasRoleManagementPermission(authentication)) {
            throw new SecurityException("Access denied");
        }
        
        UserRoleAssignmentDTO result = new UserRoleAssignmentDTO();
        
        try {
            userRoleService.unassignRoleFromUser(userId, roleId);
            result.setStatus("success");
            result.setMessage("Role berhasil di-unassign dari user");
            
        } catch (Exception e) {
            log.error("Error unassigning role from user", e);
            result.setStatus("error");
            result.setMessage("Gagal unassign role: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * API endpoint untuk mendapatkan menu yang tersedia.
     */
    @GetMapping("/api/menus")
    @ResponseBody
    @PreAuthorize("@userDTO.getRoles().contains('omnip-admin')")
    public List<MenuDTO> getAvailableMenus() {
        List<Menus> menus = menuService.getAllActiveMenus();
        return menus.stream()
                .map(this::convertToMenuDTO)
                .collect(Collectors.toList());
    }

    /**
     * API endpoint untuk search users.
     */
    @GetMapping("/api/users/search")
    @ResponseBody
    @PreAuthorize("@userDTO.getRoles().contains('omnip-admin')")
    public List<UserDTO> searchUsers(@RequestParam String query, Authentication authentication) {
        log.info("Searching users with query: {}", query);
        
        if (!hasRoleManagementPermission(authentication)) {
            throw new SecurityException("Access denied");
        }
        
        if (query.length() < 2) {
            return List.of();
        }
        
        // Search users by email, username, or fullname
        List<Users> users = usersRepository.findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrFullnameContainingIgnoreCase(
            query, query, query);
        
        return users.stream()
                .filter(user -> user.isActive() && !user.isDeleted())
                .limit(20) // Limit results to 20 users
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }

    /**
     * API endpoint untuk mendapatkan role yang dimiliki user.
     */
    @GetMapping("/api/users/{userId}/roles")
    @ResponseBody
    @PreAuthorize("@userDTO.getRoles().contains('omnip-admin')")
    public List<RoleDTO> getUserRoles(@PathVariable UUID userId, Authentication authentication) {
        log.info("Getting roles for user: {}", userId);
        
        if (!hasRoleManagementPermission(authentication)) {
            throw new SecurityException("Access denied");
        }
        
        List<Roles> userRoles = userRoleService.getUserRoles(userId);
        return userRoles.stream()
                .map(this::convertToRoleDTO)
                .collect(Collectors.toList());
    }

    /**
     * Helper method untuk convert Role entity ke RoleDTO.
     */
    private RoleDTO convertToRoleDTO(Roles role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setRoleCode(role.getRoleCode());
        dto.setRoleName(role.getRoleName());
        dto.setDescription(role.getDescription());
        dto.setActive(role.isActive());
        
        if (role.getMenus() != null) {
            List<MenuDTO> menuDTOs = role.getMenus().stream()
                    .map(this::convertToMenuDTO)
                    .collect(Collectors.toList());
            dto.setMenus(menuDTOs);
            
            List<UUID> menuIds = role.getMenus().stream()
                    .map(Menus::getId)
                    .collect(Collectors.toList());
            dto.setMenuIds(menuIds);
        }
        
        return dto;
    }

    /**
     * Helper method untuk convert Menu entity ke MenuDTO.
     */
    private MenuDTO convertToMenuDTO(Menus menu) {
        MenuDTO dto = new MenuDTO();
        dto.setId(menu.getId());
        dto.setMenuCode(menu.getMenuCode());
        dto.setMenuName(menu.getMenuName());
        dto.setDescription(menu.getDescription());
        dto.setUrl(menu.getUrl());
        dto.setIcon(menu.getIcon());
        dto.setSortOrder(menu.getSortOrder());
        dto.setActive(menu.isActive());
        
        if (menu.getParentMenu() != null) {
            dto.setParentMenuId(menu.getParentMenu().getId());
            dto.setParentMenuName(menu.getParentMenu().getMenuName());
        }
        
        return dto;
    }

    /**
     * Helper method untuk convert User entity ke UserDTO.
     */
    private UserDTO convertToUserDTO(Users user) {
        UserDTO dto = new UserDTO();
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setFullname(user.getFullname());
        dto.setProviderUserId(user.getProviderUserId());
        dto.setActive(user.isActive());
        dto.setStores(user.getStores());
        
        if (user.getRoles() != null) {
            dto.setRoles(user.getRoles());
        }
        
        return dto;
    }

    /**
     * Helper method untuk check permission role management.
     */
    private boolean hasRoleManagementPermission(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        try {
            Users currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return false;
            }
            
            // Check if user has omnip-admin role
            return userRoleService.hasRole(currentUser.getId(), "omnip-admin") ||
                   userRoleService.hasRole(currentUser.getId(), "omnip-store-admin");
                   
        } catch (Exception e) {
            log.error("Error checking role management permission", e);
            return false;
        }
    }

    /**
     * Helper method untuk mendapatkan current user dari authentication.
     */
    private Users getCurrentUser(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            String providerUserId = oidcUser.getSubject();
            return usersRepository.findByProviderUserId(providerUserId);
        }
        return null;
    }
}
