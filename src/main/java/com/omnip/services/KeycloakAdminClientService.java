package com.omnip.services;

import com.omnip.business.KeycloakAdminClientBusiness;
import com.omnip.dtos.KeycloakGroupDTO;
import com.omnip.dtos.KeycloakRoleDTO;
import com.omnip.dtos.UserDTO;
import com.omnip.exceptions.BusinessException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class KeycloakAdminClientService {
        private final Keycloak keycloak;
        private final KeycloakAdminClientBusiness keycloakAdminClientBusiness;

        @Value("${keycloak.realm}")
        private String realm;

        @Value("${keycloak.client-id}")
        private String clientId;

        public KeycloakAdminClientService(Keycloak keycloak, KeycloakAdminClientBusiness keycloakAdminClientBusiness) {
                this.keycloak = keycloak;
                this.keycloakAdminClientBusiness = keycloakAdminClientBusiness;
        }

        public List<KeycloakRoleDTO> getRoles() {
                List<KeycloakRoleDTO> roles = this.keycloak
                                .realm(this.realm)
                                .clients()
                                .findByClientId(this.clientId)
                                .stream()
                                .findFirst()
                                .map(client -> this.keycloak
                                                .realm(this.realm)
                                                .clients()
                                                .get(client.getId())
                                                .roles()
                                                .list().stream().map(KeycloakRoleDTO::fromRoleRepresentation)
                                                .toList())
                                .orElse(List.of());
                return roles;
        }

        /**
         * Mendapatkan roles yang difilter berdasarkan scope attribute.
         * Role harus memiliki attribute "scope" dengan value yang sesuai.
         * 
         * Contoh: scope=backoffice, scope=customer, scope=shared
         *
         * @param scope Nilai scope untuk filter (e.g., "backoffice", "customer")
         * @return List of KeycloakRoleDTO yang memiliki scope tersebut
         */
        public List<KeycloakRoleDTO> getRolesByScope(String scope) {
                return this.keycloak
                                .realm(this.realm)
                                .clients()
                                .findByClientId(this.clientId)
                                .stream()
                                .findFirst()
                                .map(client -> this.keycloak
                                                .realm(this.realm)
                                                .clients()
                                                .get(client.getId())
                                                .roles()
                                                .list()
                                                .stream()
                                                .map(role -> {
                                                        // Fetch full role with attributes
                                                        RoleRepresentation fullRole = this.keycloak
                                                                        .realm(this.realm)
                                                                        .clients()
                                                                        .get(client.getId())
                                                                        .roles()
                                                                        .get(role.getName())
                                                                        .toRepresentation();
                                                        return KeycloakRoleDTO.fromRoleRepresentation(fullRole);
                                                })
                                                .filter(roleDto -> {
                                                        var attrs = roleDto.getAttributes();
                                                        if (attrs == null)
                                                                return false;
                                                        var scopeList = attrs.get("scope");
                                                        return scopeList != null && scopeList.contains(scope);
                                                })
                                                .toList())
                                .orElse(List.of());
        }

        /**
         * Mendapatkan full role representation dengan attributes (cached).
         * Cache key adalah roleName, TTL mengikuti fastCacheManager config.
         *
         * @param roleName Nama role yang akan di-fetch
         * @return KeycloakRoleDTO dengan attributes lengkap
         */
        @Cacheable(value = "keycloakRoles", key = "#roleName", cacheManager = "fastCacheManager")
        public KeycloakRoleDTO getCachedRoleWithAttributes(String roleName) {
                log.debug("Fetching role details from Keycloak for: {}", roleName);
                ClientResource clientResource = this.keycloak
                                .realm(this.realm)
                                .clients()
                                .findByClientId(this.clientId)
                                .stream()
                                .findFirst()
                                .map(client -> this.keycloak
                                                .realm(this.realm)
                                                .clients()
                                                .get(client.getId()))
                                .orElse(null);

                if (clientResource == null) {
                        log.warn("Client not found, returning empty role for: {}", roleName);
                        return KeycloakRoleDTO.builder().name(roleName).build();
                }

                try {
                        RoleRepresentation fullRole = clientResource.roles()
                                        .get(roleName)
                                        .toRepresentation();
                        return KeycloakRoleDTO.fromRoleRepresentation(fullRole);
                } catch (Exception e) {
                        log.warn("Failed to fetch role details for: {}", roleName, e);
                        return KeycloakRoleDTO.builder().name(roleName).build();
                }
        }

        /**
         * Mendapatkan semua groups dari Keycloak
         *
         * @return List of KeycloakGroupDTO
         */
        public List<KeycloakGroupDTO> getGroups() {
                return this.keycloak
                                .realm(this.realm)
                                .groups()
                                .groups()
                                .stream()
                                .map(KeycloakGroupDTO::fromGroupRepresentation)
                                .toList();
        }

        /**
         * Mendapatkan client roles yang di-assign ke suatu group
         *
         * @param groupId ID dari group
         * @return List of KeycloakRoleDTO
         */
        public List<KeycloakRoleDTO> getRolesByGroup(String groupId) throws BusinessException {
                // Cari client resource untuk mendapatkan client ID
                ClientResource clientResource = this.keycloak
                                .realm(this.realm)
                                .clients()
                                .findByClientId(this.clientId)
                                .stream()
                                .findFirst()
                                .map(client -> this.keycloak
                                                .realm(this.realm)
                                                .clients()
                                                .get(client.getId()))
                                .orElseThrow(() -> new BusinessException("Client not found: " + this.clientId));

                // Get client level roles dari group
                return this.keycloak
                                .realm(this.realm)
                                .groups()
                                .group(groupId)
                                .roles()
                                .clientLevel(clientResource.toRepresentation().getId())
                                .listEffective()
                                .stream()
                                .map(KeycloakRoleDTO::fromRoleRepresentation)
                                .toList();
        }

        /**
         * Get all users from Keycloak realm.
         *
         * @param maxResults Maximum number of users to return (default 100)
         * @return List of UserDTO
         */
        public List<UserDTO> getAllKeycloakUsers(int maxResults) {
                return this.keycloak
                                .realm(this.realm)
                                .users()
                                .list(0, maxResults)
                                .stream()
                                .map(user -> {
                                        UserDTO dto = new UserDTO();
                                        dto.setProviderUserId(user.getId());
                                        dto.setUsername(user.getUsername());
                                        dto.setEmail(user.getEmail());
                                        dto.setFullname(user.getFirstName() != null ? (user.getFirstName() + " "
                                                        + (user.getLastName() != null ? user.getLastName() : "")).trim()
                                                        : user.getUsername());
                                        dto.setActive(user.isEnabled());
                                        return dto;
                                })
                                .toList();
        }

        /**
         * Assign client role ke group
         *
         * @param groupId  ID dari group
         * @param roleName Nama role yang akan di-assign
         */
        public void assignRoleToGroup(String groupId, String roleName) throws BusinessException {
                // Cari client resource
                ClientResource clientResource = this.keycloak
                                .realm(this.realm)
                                .clients()
                                .findByClientId(this.clientId)
                                .stream()
                                .findFirst()
                                .map(client -> this.keycloak
                                                .realm(this.realm)
                                                .clients()
                                                .get(client.getId()))
                                .orElseThrow(() -> new BusinessException("Client not found: " + this.clientId));

                // Get role representation
                RoleRepresentation role = clientResource
                                .roles()
                                .get(roleName)
                                .toRepresentation();

                // Assign role ke group (client level)
                this.keycloak
                                .realm(this.realm)
                                .groups()
                                .group(groupId)
                                .roles()
                                .clientLevel(clientResource.toRepresentation().getId())
                                .add(List.of(role));

                log.info("Role '{}' assigned to group '{}'", roleName, groupId);
        }

        /**
         * Unassign/remove client role dari group
         *
         * @param groupId  ID dari group
         * @param roleName Nama role yang akan di-remove
         */
        public void unassignRoleFromGroup(String groupId, String roleName) throws BusinessException {
                // Cari client resource
                ClientResource clientResource = this.keycloak
                                .realm(this.realm)
                                .clients()
                                .findByClientId(this.clientId)
                                .stream()
                                .findFirst()
                                .map(client -> this.keycloak
                                                .realm(this.realm)
                                                .clients()
                                                .get(client.getId()))
                                .orElseThrow(() -> new BusinessException("Client not found: " + this.clientId));

                // Get role representation
                RoleRepresentation role = clientResource
                                .roles()
                                .get(roleName)
                                .toRepresentation();

                // Remove role dari group (client level)
                this.keycloak
                                .realm(this.realm)
                                .groups()
                                .group(groupId)
                                .roles()
                                .clientLevel(clientResource.toRepresentation().getId())
                                .remove(List.of(role));

                log.info("Role '{}' removed from group '{}'", roleName, groupId);
        }

        public void updateUser(String providerUserId, UserRepresentation userRepresentation) {
                UsersResource usersResource = this.keycloak.realm(this.realm).users();
                usersResource.get(providerUserId).update(userRepresentation);
        }

        public void changeUserPassword(String userId, String newPassword) {
                UserRepresentation userRep = this.keycloakAdminClientBusiness
                                .preparePasswordUpdateRepresentation(newPassword);
                this.updateUser(userId, userRep);
        }

        public String createUser(String username, String fullname, String email, String password, String requestedRole)
                        throws BusinessException {
                UserRepresentation userRep = this.keycloakAdminClientBusiness.prepareUserRepresentation(username,
                                fullname,
                                email);
                Response resp = this.keycloak.realm(this.realm).users().create(userRep);
                String createdUserId = this.keycloakAdminClientBusiness.extractCreatedUserId(resp);
                CredentialRepresentation cred = this.keycloakAdminClientBusiness.preparePasswordCredential(password,
                                true);
                this.keycloak.realm(this.realm)
                                .users()
                                .get(createdUserId)
                                .resetPassword(cred);
                ClientResource clientRes = this.keycloak.realm(this.realm)
                                .clients()
                                .findByClientId(this.clientId)
                                .stream()
                                .findFirst()
                                .map(id -> this.keycloak.realm(this.realm).clients().get(id.getId()))
                                .orElseThrow();

                RoleRepresentation role = clientRes.roles()
                                .get(requestedRole)
                                .toRepresentation();

                this.keycloak.realm(this.realm)
                                .users()
                                .get(createdUserId)
                                .roles()
                                .clientLevel(clientRes.toRepresentation().getId())
                                .add(List.of(role));

                return createdUserId;
        }

        public void updateUserStatus(String userId, boolean isEnabled) {
                UserRepresentation userRep = this.keycloakAdminClientBusiness
                                .prepareStatusUpdateRepresentation(isEnabled);
                this.updateUser(userId, userRep);
        }

        // ==================== User-Group Management ====================

        /**
         * Assign user ke group
         *
         * @param userId  ID dari user di Keycloak
         * @param groupId ID dari group
         */
        public void assignUserToGroup(String userId, String groupId) {
                this.keycloak
                                .realm(this.realm)
                                .users()
                                .get(userId)
                                .joinGroup(groupId);
                log.info("User '{}' assigned to group '{}'", userId, groupId);
        }

        /**
         * Remove user dari group
         *
         * @param userId  ID dari user di Keycloak
         * @param groupId ID dari group
         */
        public void removeUserFromGroup(String userId, String groupId) {
                this.keycloak
                                .realm(this.realm)
                                .users()
                                .get(userId)
                                .leaveGroup(groupId);
                log.info("User '{}' removed from group '{}'", userId, groupId);
        }

        /**
         * Mendapatkan groups yang dimiliki user
         *
         * @param userId ID dari user di Keycloak
         * @return List of KeycloakGroupDTO
         */
        public List<KeycloakGroupDTO> getUserGroups(String userId) {
                return this.keycloak
                                .realm(this.realm)
                                .users()
                                .get(userId)
                                .groups()
                                .stream()
                                .map(KeycloakGroupDTO::fromGroupRepresentation)
                                .toList();
        }

        /**
         * Mendapatkan members dari suatu group
         *
         * @param groupId ID dari group
         * @return List of UserRepresentation (basic info)
         */
        public List<UserRepresentation> getGroupMembers(String groupId) {
                return getGroupMembers(groupId, false);
        }

        /**
         * Mendapatkan members dari suatu group dengan opsi recursive
         *
         * @param groupId   ID dari group
         * @param recursive Jika true, akan mengambil members dari semua subgroups
         * @return List of UserRepresentation (basic info)
         */
        public List<UserRepresentation> getGroupMembers(String groupId, boolean recursive) {
                if (!recursive) {
                        return this.keycloak
                                        .realm(this.realm)
                                        .groups()
                                        .group(groupId)
                                        .members();
                }

                // Recursive fetch
                java.util.Set<UserRepresentation> allUsers = new java.util.HashSet<>();
                collectGroupMembersRecursively(groupId, allUsers);
                return new java.util.ArrayList<>(allUsers);
        }

        private void collectGroupMembersRecursively(String groupId, java.util.Set<UserRepresentation> allUsers) {
                // Add direct members of this group
                allUsers.addAll(this.keycloak
                                .realm(this.realm)
                                .groups()
                                .group(groupId)
                                .members());

                // Fetch subgroups using new API (Keycloak 23+)
                List<GroupRepresentation> subGroups = this.keycloak
                                .realm(this.realm)
                                .groups()
                                .group(groupId)
                                .getSubGroups(0, Integer.MAX_VALUE, false);

                // Recursively process subgroups
                for (GroupRepresentation sub : subGroups) {
                        collectGroupMembersRecursively(sub.getId(), allUsers);
                }
        }

        /**
         * Mendapatkan semua groups dengan hierarchy (parent-child)
         * Uses GroupResource.getSubGroups() for Keycloak 23+ compatibility.
         *
         * @return List of KeycloakGroupDTO dengan subGroups populated
         */
        public List<KeycloakGroupDTO> getGroupsHierarchy() {
                // Get top-level groups first
                List<GroupRepresentation> topLevelGroups = this.keycloak
                                .realm(this.realm)
                                .groups()
                                .groups();

                // For each top-level group, build hierarchy using getSubGroups()
                return topLevelGroups.stream()
                                .map(group -> buildGroupHierarchy(group.getId(), null))
                                .toList();
        }

        /**
         * Recursively build group hierarchy using getSubGroups() API
         * Required for Keycloak 23+ where toRepresentation() no longer includes
         * subGroups
         */
        private KeycloakGroupDTO buildGroupHierarchy(String groupId, String parentId) {
                // Fetch group details
                GroupRepresentation group = this.keycloak
                                .realm(this.realm)
                                .groups()
                                .group(groupId)
                                .toRepresentation();

                // Build DTO
                KeycloakGroupDTO dto = KeycloakGroupDTO.builder()
                                .id(group.getId())
                                .name(group.getName())
                                .path(group.getPath())
                                .parentId(parentId)
                                .subGroups(new java.util.ArrayList<>())
                                .build();

                // Fetch subgroups using new API (Keycloak 23+)
                List<GroupRepresentation> subGroups = this.keycloak
                                .realm(this.realm)
                                .groups()
                                .group(groupId)
                                .getSubGroups(0, Integer.MAX_VALUE, false);

                // Recursively build children
                for (GroupRepresentation sub : subGroups) {
                        dto.getSubGroups().add(buildGroupHierarchy(sub.getId(), group.getId()));
                }

                return dto;
        }

        /**
         * Mendapatkan subgroups dari suatu parent group
         *
         * @param parentPath Path dari parent group (e.g., "/backoffice")
         * @return List of KeycloakGroupDTO (flat list of subgroups only)
         */
        public List<KeycloakGroupDTO> getSubGroups(String parentPath) {
                return this.keycloak
                                .realm(this.realm)
                                .groups()
                                .groups()
                                .stream()
                                .filter(group -> parentPath.equals(group.getPath()))
                                .findFirst()
                                .map(parent -> parent.getSubGroups().stream()
                                                .map(KeycloakGroupDTO::fromGroupRepresentation)
                                                .toList())
                                .orElse(List.of());
        }

        // ==================== Role Attributes ====================

        /**
         * Update attributes dari role
         *
         * @param roleName   Nama role
         * @param attributes Map of attribute key to list of values
         */
        public void updateRoleAttributes(String roleName, java.util.Map<String, java.util.List<String>> attributes)
                        throws BusinessException {
                ClientResource clientResource = this.keycloak
                                .realm(this.realm)
                                .clients()
                                .findByClientId(this.clientId)
                                .stream()
                                .findFirst()
                                .map(client -> this.keycloak
                                                .realm(this.realm)
                                                .clients()
                                                .get(client.getId()))
                                .orElseThrow(() -> new BusinessException("Client not found: " + this.clientId));

                RoleRepresentation role = clientResource.roles()
                                .get(roleName)
                                .toRepresentation();

                role.setAttributes(attributes);

                clientResource.roles()
                                .get(roleName)
                                .update(role);

                log.info("Role '{}' attributes updated: {}", roleName, attributes.keySet());
        }

        /**
         * Mendapatkan client roles yang dimiliki oleh user, dikelompokkan berdasarkan
         * hierarchy (composite roles).
         *
         * @param userId ID dari user di Keycloak
         * @return List of KeycloakRoleDTO (Root roles only)
         */
        public List<KeycloakRoleDTO> getRolesByUser(String userId) throws BusinessException {
                // Cari client resource untuk mendapatkan client ID
                ClientResource clientResource = this.keycloak
                                .realm(this.realm)
                                .clients()
                                .findByClientId(this.clientId)
                                .stream()
                                .findFirst()
                                .map(client -> this.keycloak
                                                .realm(this.realm)
                                                .clients()
                                                .get(client.getId()))
                                .orElseThrow(() -> new BusinessException("Client not found: " + this.clientId));

                String clientUuid = clientResource.toRepresentation().getId();

                // 1. Get ALL client level roles assigned/effective to user
                List<RoleRepresentation> allUserRoles = this.keycloak
                                .realm(this.realm)
                                .users()
                                .get(userId)
                                .roles()
                                .clientLevel(clientUuid)
                                .listEffective();

                // 2. Fetch FULL role representation (with attributes) using CACHED method
                // listEffective() does not include attributes, so we use cached method
                java.util.Map<String, KeycloakRoleDTO> dtoMap = new java.util.HashMap<>();
                for (RoleRepresentation minimalRole : allUserRoles) {
                        KeycloakRoleDTO cachedRole = getCachedRoleWithAttributes(minimalRole.getName());
                        dtoMap.put(cachedRole.getName(), cachedRole);
                }

                // 3. Build hierarchy using cached DTOs
                // Iterate over all roles. If a role is composite, find its children and add
                // them.
                java.util.Set<String> childRoleNames = new java.util.HashSet<>();

                for (KeycloakRoleDTO parentDto : dtoMap.values()) {
                        if (Boolean.TRUE.equals(parentDto.getComposite())) {
                                try {
                                        // Fetch children of this composite role
                                        java.util.Set<RoleRepresentation> children = clientResource.roles()
                                                        .get(parentDto.getName())
                                                        .getRoleComposites();

                                        for (RoleRepresentation child : children) {
                                                // Get cached child with attributes
                                                KeycloakRoleDTO childDto = getCachedRoleWithAttributes(child.getName());

                                                // Only add if child is in user's role list
                                                if (dtoMap.containsKey(child.getName())) {
                                                        parentDto.getChildren().add(childDto);
                                                        childRoleNames.add(child.getName());
                                                }
                                        }
                                } catch (Exception e) {
                                        log.warn("Failed to fetch composites for role: {}", parentDto.getName());
                                }
                        }
                }

                // 3. Return only roles that are NOT children of any other role in this list
                // (Roots)
                // Also sort them if needed? For now, we rely on map iteration order or list
                // order from Keycloak
                return dtoMap.values().stream()
                                .filter(dto -> !childRoleNames.contains(dto.getName()))
                                .toList();
        }
}