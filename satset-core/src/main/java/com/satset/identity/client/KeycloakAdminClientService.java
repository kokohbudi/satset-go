package com.satset.identity.client;

import com.satset.identity.model.GroupMemberInfo;
import com.satset.identity.model.KeycloakGroup;
import com.satset.identity.model.KeycloakRole;
import com.satset.shared.dto.GroupInfo;
import com.satset.shared.dto.RoleInfo;
import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.BusinessException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class KeycloakAdminClientService implements KeycloakIdentityPort, KeycloakOrganizationPort {
        private final Keycloak keycloak;
        private final KeycloakHelper keycloakAdminClientBusiness;
        private final IdentityMapper identityMapper;

        @Value("${keycloak.realm}")
        private String realm;

        @Value("${keycloak.client-id}")
        private String clientId;

        public KeycloakAdminClientService(Keycloak keycloak, KeycloakHelper keycloakAdminClientBusiness, IdentityMapper identityMapper) {
                this.keycloak = keycloak;
                this.keycloakAdminClientBusiness = keycloakAdminClientBusiness;
                this.identityMapper = identityMapper;
        }

        /**
         * Mendapatkan semua REALM roles (excluding system roles).
         * Updated for new structure: roles are now realm-level.
         * CACHED: 5 minutes TTL untuk menghindari repeated API calls.
         */
        @Cacheable(value = "allRoles", cacheManager = "fastCacheManager")
        public List<KeycloakRole> getRoles() {
                log.debug("Fetching all realm roles from Keycloak (cache miss)");
                java.util.Set<String> systemRoles = java.util.Set.of(
                                "offline_access", "uma_authorization", "default-roles-satset-go",
                                "default-roles-omnip");
                return this.keycloak
                                .realm(this.realm)
                                .roles()
                                .list()
                                .stream()
                                .filter(role -> !systemRoles.contains(role.getName()))
                                .map(KeycloakRole::fromRoleRepresentation)
                                .toList();
        }

        /**
         * Get ALL roles with hierarchy structure.
         * Composite roles will have their children array populated.
         * Non-composite roles will have empty children array.
         * Used for dropdown display with visual hierarchy.
         * CACHED: 5 minutes TTL untuk menghindari repeated API calls.
         *
         * @return List of KeycloakRole with hierarchy info
         */
        @Cacheable(value = "rolesHierarchy", cacheManager = "fastCacheManager")
        public List<KeycloakRole> getRolesWithHierarchy() {
                log.debug("Fetching roles with hierarchy from Keycloak (cache miss)");
                java.util.Set<String> systemRoles = java.util.Set.of(
                                "offline_access", "uma_authorization", "default-roles-satset-go",
                                "default-roles-omnip");

                List<RoleRepresentation> allRoles = this.keycloak
                                .realm(this.realm)
                                .roles()
                                .list();

                // Build a map of role name -> DTO with children populated
                java.util.Map<String, KeycloakRole> roleMap = new java.util.LinkedHashMap<>();

                // First pass: create DTOs and collect composite children
                for (RoleRepresentation role : allRoles) {
                        if (systemRoles.contains(role.getName())) {
                                continue;
                        }

                        KeycloakRole dto = getCachedRoleWithAttributes(role.getName());
                        dto.setChildren(new java.util.ArrayList<>());

                        if (role.isComposite()) {
                                try {
                                        java.util.Set<RoleRepresentation> children = this.keycloak
                                                        .realm(this.realm)
                                                        .roles()
                                                        .get(role.getName())
                                                        .getRoleComposites();
                                        for (RoleRepresentation child : children) {
                                                if (!systemRoles.contains(child.getName())) {
                                                        KeycloakRole childDto = getCachedRoleWithAttributes(
                                                                        child.getName());
                                                        childDto.setChildren(new java.util.ArrayList<>());
                                                        dto.getChildren().add(childDto);
                                                }
                                        }
                                } catch (Exception e) {
                                        log.warn("Failed to fetch composites for role: {}", role.getName());
                                }
                        }
                        roleMap.put(role.getName(), dto);
                }

                // Return all roles - composite roles have children populated
                return new java.util.ArrayList<>(roleMap.values());
        }

        /**
         * Get child role names from a composite role.
         * Used for filtering users by parent role.
         * 
         * @param compositeRoleName Name of the composite role
         * @return Set of child role names
         */
        public java.util.Set<String> getCompositeRoleChildNames(String compositeRoleName) {
                java.util.Set<String> childNames = new java.util.HashSet<>();
                try {
                        java.util.Set<RoleRepresentation> children = this.keycloak
                                        .realm(this.realm)
                                        .roles()
                                        .get(compositeRoleName)
                                        .getRoleComposites();
                        for (RoleRepresentation child : children) {
                                childNames.add(child.getName());
                        }
                } catch (Exception e) {
                        log.warn("Failed to fetch composites for role: {}", compositeRoleName);
                }
                return childNames;
        }

        /**
         * Mendapatkan roles yang difilter berdasarkan scope attribute.
         * Role harus memiliki attribute "scope" dengan value yang sesuai.
         * <p>Contoh: scope=backoffice, scope=customer, scope=shared
         *
         * @param scope Nilai scope untuk filter (e.g., "backoffice", "customer")
         * @return List of KeycloakRole yang memiliki scope tersebut
         */
        public List<KeycloakRole> getRolesByScope(String scope) {
                java.util.Set<String> systemRoles = java.util.Set.of(
                                "offline_access", "uma_authorization", "default-roles-satset-go",
                                "default-roles-omnip");
                return this.keycloak
                                .realm(this.realm)
                                .roles()
                                .list()
                                .stream()
                                .filter(role -> !systemRoles.contains(role.getName()))
                                .map(role -> getCachedRoleWithAttributes(role.getName()))
                                .filter(roleDto -> {
                                        var attrs = roleDto.getAttributes();
                                        if (attrs == null)
                                                return false;
                                        var scopeValue = attrs.get("scope");
                                        return scopeValue != null && scopeValue.equals(scope);
                                })
                                .toList();
        }

        /**
         * Mendapatkan full REALM role representation dengan attributes (cached).
         * Updated for new structure: roles are now realm-level.
         */
        @Cacheable(value = "keycloakRoles", key = "#roleName", cacheManager = "fastCacheManager")
        public KeycloakRole getCachedRoleWithAttributes(String roleName) {
                log.debug("Fetching realm role details from Keycloak for: {}", roleName);
                try {
                        RoleRepresentation fullRole = this.keycloak
                                        .realm(this.realm)
                                        .roles()
                                        .get(roleName)
                                        .toRepresentation();
                        return KeycloakRole.fromRoleRepresentation(fullRole);
                } catch (Exception e) {
                        log.warn("Failed to fetch realm role details for: {}", roleName, e);
                        return KeycloakRole.builder().name(roleName).build();
                }
        }

        /**
         * Mendapatkan full CLIENT role representation dengan attributes (cached).
         * Cache key menggunakan prefix "client:" untuk menghindari collision dengan realm roles.
         *
         * @param clientUuid UUID internal client di Keycloak (bukan clientId string)
         * @param roleName   Nama client role
         * @return KeycloakRole dengan attributes lengkap
         */
        @Cacheable(value = "keycloakRoles", key = "'client:' + #clientUuid + ':' + #roleName", cacheManager = "fastCacheManager")
        public KeycloakRole getCachedClientRoleWithAttributes(String clientUuid, String roleName) {
                log.debug("Fetching client role details from Keycloak for: {}/{}", clientUuid, roleName);
                try {
                        RoleRepresentation fullRole = this.keycloak
                                .realm(this.realm)
                                .clients()
                                .get(clientUuid)
                                .roles()
                                .get(roleName)
                                .toRepresentation();
                        return KeycloakRole.fromRoleRepresentation(fullRole);
                } catch (Exception e) {
                        log.warn("Failed to fetch client role details for: {}/{}", clientUuid, roleName, e);
                        return KeycloakRole.builder().name(roleName).clientRole(true).build();
                }
        }

        /**
         * Mencari UUID internal dari client berdasarkan clientId string yang dikonfigurasi.
         *
         * @return UUID internal client, atau null jika tidak ditemukan
         */
        private String findClientUuid() {
                return this.keycloak
                        .realm(this.realm)
                        .clients()
                        .findByClientId(this.clientId)
                        .stream()
                        .findFirst()
                        .map(ClientRepresentation::getId)
                        .orElse(null);
        }

        /**
         * Mendapatkan semua groups dari Keycloak (cached).
         * Cache TTL: 5 menit (fastCacheManager).
         *
         * @return List of KeycloakGroup
         */
        @Cacheable(value = "keycloakGroups", cacheManager = "fastCacheManager")
        public List<KeycloakGroup> getGroups() {
                log.debug("Fetching groups from Keycloak (cache miss)");
                return this.keycloak
                                .realm(this.realm)
                                .groups()
                                .groups()
                                .stream()
                                .map(KeycloakGroup::fromGroupRepresentation)
                                .toList();
        }

        /**
         * Mendapatkan client roles yang di-assign ke suatu group
         *
         * @param groupId ID dari group
         * @return List of KeycloakRole
         */
        public List<KeycloakRole> getRolesByGroup(String groupId) throws BusinessException {
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
                                .map(KeycloakRole::fromRoleRepresentation)
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
         * Get all users with their roles enriched using VIRTUAL THREADS.
         * OPTIMIZED: Uses Virtual Threads for high-concurrency fetching.
         * ideal for I/O bound operations like Keycloak API calls.
         *
         * @param maxResults Maximum number of users to return
         * @return List of UserDTO with roleDetails populated
         */
        public List<UserDTO> getUsersWithRolesBatch(int maxResults) {
                log.debug("Batch fetching users with roles using VIRTUAL THREADS");
                long startTime = System.currentTimeMillis();

                // Step 1: Fetch all users (single API call)
                List<UserDTO> users = getAllKeycloakUsers(maxResults);

                // Step 2: Enrich with roles using Virtual Threads
                // Virtual threads are lightweight and perfect for I/O waiting (Keycloak calls)
                // This try-with-resources block waits for all threads to complete
                try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
                        users.forEach(user -> executor.submit(() -> {
                                try {
                                        List<KeycloakRole> roles = getAllEffectiveRolesFlat(
                                                        user.getProviderUserId());
                                        // Convert to RoleInfo for shared DTO
                                        user.setRoleDetails(identityMapper.toRoleInfoList(roles));
                                } catch (Exception e) {
                                        log.warn("Failed to fetch roles for user: {}", user.getEmail());
                                        user.setRoleDetails(List.of());
                                }
                        }));
                }

                long elapsed = System.currentTimeMillis() - startTime;
                log.info("Batch fetched {} users with roles in {}ms (Virtual Threads)", users.size(), elapsed);

                return users;
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

        /**
         * Assign realm role to user.
         *
         * @param userId   ID of the user
         * @param roleName Name of the realm role to assign
         */
        public void assignRoleToUser(String userId, String roleName) {
                // Get realm role
                RoleRepresentation role = this.keycloak
                                .realm(this.realm)
                                .roles()
                                .get(roleName)
                                .toRepresentation();

                // Assign role to user
                this.keycloak
                                .realm(this.realm)
                                .users()
                                .get(userId)
                                .roles()
                                .realmLevel()
                                .add(List.of(role));

                log.info("Role '{}' assigned to user '{}'", roleName, userId);
        }

        /**
         * Assign client role to user.
         *
         * @param userId   ID of the user
         * @param roleName Name of the client role to assign
         */
        public void assignClientRoleToUser(String userId, String roleName) throws BusinessException {
                log.info("assignClientRoleToUser: looking for clientId='{}' in realm='{}'", this.clientId, this.realm);
                var foundClients = this.keycloak
                                .realm(this.realm)
                                .clients()
                                .findByClientId(this.clientId);
                log.info("assignClientRoleToUser: findByClientId returned {} clients: {}",
                                foundClients.size(),
                                foundClients.stream().map(c -> c.getClientId() + " (id=" + c.getId() + ")").toList());

                ClientResource clientResource = foundClients
                                .stream()
                                .findFirst()
                                .map(client -> this.keycloak
                                                .realm(this.realm)
                                                .clients()
                                                .get(client.getId()))
                                .orElseThrow(() -> new BusinessException("Client not found: " + this.clientId));

                RoleRepresentation role = clientResource
                                .roles()
                                .get(roleName)
                                .toRepresentation();

                this.keycloak
                                .realm(this.realm)
                                .users()
                                .get(userId)
                                .roles()
                                .clientLevel(clientResource.toRepresentation().getId())
                                .add(List.of(role));

                log.info("Client role '{}' assigned to user '{}'", roleName, userId);
        }

        /**
         * Remove realm role from user.
         *
         * @param userId   ID of the user
         * @param roleName Name of the realm role to remove
         */
        public void unassignRoleFromUser(String userId, String roleName) {
                // Get realm role
                RoleRepresentation role = this.keycloak
                                .realm(this.realm)
                                .roles()
                                .get(roleName)
                                .toRepresentation();

                // Remove role from user
                this.keycloak
                                .realm(this.realm)
                                .users()
                                .get(userId)
                                .roles()
                                .realmLevel()
                                .remove(List.of(role));

                log.info("Role '{}' removed from user '{}'", roleName, userId);
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

        /**
         * Create a new Backoffice User in Keycloak.
         * Sets password and assigns initial realm role.
         */
        public String createBackofficeUser(String username, String fullname, String email, String password,
                        String requestedRole)
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

                // Assign REALM role instead of Client role
                if (requestedRole != null && !requestedRole.isEmpty()) {
                        assignRoleToUser(createdUserId, requestedRole);
                }

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
         * @return List of KeycloakGroup
         */
        public List<KeycloakGroup> getUserGroups(String userId) {
                return this.keycloak
                                .realm(this.realm)
                                .users()
                                .get(userId)
                                .groups()
                                .stream()
                                .map(KeycloakGroup::fromGroupRepresentation)
                                .toList();
        }

        /**
         * Mendapatkan members dari suatu group dengan opsi recursive.
         * Converts Keycloak UserRepresentation to domain GroupMemberInfo at the boundary.
         */
        public List<GroupMemberInfo> getGroupMembers(String groupId, boolean recursive) {
                List<UserRepresentation> rawMembers;
                if (!recursive) {
                        rawMembers = this.keycloak
                                        .realm(this.realm)
                                        .groups()
                                        .group(groupId)
                                        .members();
                } else {
                        java.util.Set<UserRepresentation> allUsers = new java.util.HashSet<>();
                        collectGroupMembersRecursively(groupId, allUsers);
                        rawMembers = new java.util.ArrayList<>(allUsers);
                }
                return rawMembers.stream().map(this::toGroupMemberInfo).toList();
        }

        private GroupMemberInfo toGroupMemberInfo(UserRepresentation userRep) {
                String fullname = (userRep.getFirstName() != null ? userRep.getFirstName() : "")
                        + (userRep.getLastName() != null ? " " + userRep.getLastName() : "");
                return new GroupMemberInfo(
                        userRep.getId(),
                        userRep.getUsername(),
                        fullname.trim(),
                        userRep.getEmail(),
                        userRep.isEnabled());
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
         * Mendapatkan semua groups dengan hierarchy (parent-child) - CACHED.
         * Uses GroupResource.getSubGroups() for Keycloak 23+ compatibility.
         * Cache TTL: 5 menit (fastCacheManager).
         *
         * @return List of KeycloakGroup dengan subGroups populated
         */
        @Cacheable(value = "groupsHierarchy", cacheManager = "fastCacheManager")
        public List<KeycloakGroup> getGroupsHierarchy() {
                log.debug("Fetching groups hierarchy from Keycloak (cache miss)");
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
        private KeycloakGroup buildGroupHierarchy(String groupId, String parentId) {
                // Fetch group details
                GroupRepresentation group = this.keycloak
                                .realm(this.realm)
                                .groups()
                                .group(groupId)
                                .toRepresentation();

                // Build DTO
                KeycloakGroup dto = KeycloakGroup.builder()
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
         * @return List of KeycloakGroup (flat list of subgroups only)
         */
        public List<KeycloakGroup> getSubGroups(String parentPath) {
                return this.keycloak
                                .realm(this.realm)
                                .groups()
                                .groups()
                                .stream()
                                .filter(group -> parentPath.equals(group.getPath()))
                                .findFirst()
                                .map(parent -> parent.getSubGroups().stream()
                                                .map(KeycloakGroup::fromGroupRepresentation)
                                                .toList())
                                .orElse(List.of());
        }

        /**
         * Get DIRECTLY ASSIGNED (granular) realm roles for a user.
         * Used for User Management display - shows only roles assigned directly to
         * user,
         * NOT roles inherited from composite roles.
         * <p>Example:
         * - User has manage_users (composite containing create_users, delete_users)
         * - This returns: [manage_users] only
         * - If user also has create_users assigned directly, returns: [manage_users,
         * create_users]
         *
         * @param userId ID dari user di Keycloak
         * @return List of KeycloakRole (directly assigned roles only, excluding
         *         system roles)
         */
        public List<KeycloakRole> getAllEffectiveRolesFlat(String userId) {
                java.util.Set<String> systemRoles = java.util.Set.of(
                                "offline_access", "uma_authorization", "default-roles-satset-go",
                                "default-roles-omnip");

                try {
                        // Use listAll() instead of listEffective() to get only directly assigned roles
                        // listEffective() returns all roles including those inherited from composite
                        // roles
                        // listAll() returns only roles that are explicitly/granularly assigned
                        List<RoleRepresentation> directlyAssignedRoles = this.keycloak
                                        .realm(this.realm)
                                        .users()
                                        .get(userId)
                                        .roles()
                                        .realmLevel()
                                        .listAll();

                        return directlyAssignedRoles.stream()
                                        .filter(role -> !systemRoles.contains(role.getName()))
                                        .map(role -> getCachedRoleWithAttributes(role.getName()))
                                        .toList();
                } catch (Exception e) {
                        log.warn("Failed to fetch roles for user: {}", userId);
                        return List.of();
                }
        }

        /**
         * Get user roles for SIDEBAR MENU display.
         * Returns root roles with children nested (hierarchical structure).
         * DO NOT use this for flat role listing - use getAllEffectiveRolesFlat instead.
         *
         * @param userId ID dari user di Keycloak
         * @return List of KeycloakRole with hierarchy (root roles only, children
         *         nested)
         */
        public List<KeycloakRole> getMenuRoles(String userId) {
                java.util.Set<String> systemRoles = java.util.Set.of(
                                "offline_access", "uma_authorization", "default-roles-satset-go",
                                "default-roles-omnip");

                // Fetch FULL role representation (with attributes) using CACHED method
                // listEffective() does not include attributes!
                java.util.Map<String, KeycloakRole> dtoMap = new java.util.HashMap<>();

                // 1. Get realm level roles
                List<RoleRepresentation> realmRoles = this.keycloak
                        .realm(this.realm)
                        .users()
                        .get(userId)
                        .roles()
                        .realmLevel()
                        .listEffective();

                for (RoleRepresentation role : realmRoles) {
                        if (systemRoles.contains(role.getName())) continue;
                        KeycloakRole fullRole = getCachedRoleWithAttributes(role.getName());
                        if (fullRole.getChildren() == null) fullRole.setChildren(new java.util.ArrayList<>());
                        dtoMap.put(role.getName(), fullRole);
                }

                // 2. Get client level roles and merge
                String clientUuid = findClientUuid();
                if (clientUuid != null) {
                        try {
                                List<RoleRepresentation> clientRoles = this.keycloak
                                        .realm(this.realm)
                                        .users()
                                        .get(userId)
                                        .roles()
                                        .clientLevel(clientUuid)
                                        .listEffective();

                                for (RoleRepresentation role : clientRoles) {
                                        if (systemRoles.contains(role.getName())) continue;
                                        KeycloakRole fullRole = getCachedClientRoleWithAttributes(clientUuid, role.getName());
                                        if (fullRole.getChildren() == null)
                                                fullRole.setChildren(new java.util.ArrayList<>());
                                        // Use qualified key to avoid name collision with realm roles
                                        dtoMap.put("client:" + role.getName(), fullRole);
                                }
                        } catch (Exception e) {
                                log.warn("Failed to fetch client roles for user: {}", userId);
                        }
                }

                // 3. Build hierarchy for composite roles
                java.util.Set<String> childRoleNames = new java.util.HashSet<>();

                for (KeycloakRole parentDto : dtoMap.values()) {
                        if (!Boolean.TRUE.equals(parentDto.getComposite())) continue;
                        try {
                                java.util.Set<RoleRepresentation> children;
                                if (Boolean.TRUE.equals(parentDto.getClientRole()) && clientUuid != null) {
                                        children = this.keycloak
                                                .realm(this.realm)
                                                .clients()
                                                .get(clientUuid)
                                                .roles()
                                                .get(parentDto.getName())
                                                .getRoleComposites();
                                } else {
                                        children = this.keycloak
                                                .realm(this.realm)
                                                .roles()
                                                        .get(parentDto.getName())
                                                        .getRoleComposites();
                                }

                                for (RoleRepresentation child : children) {
                                        if (systemRoles.contains(child.getName())) continue;
                                        KeycloakRole childDto = Boolean.TRUE.equals(child.getClientRole()) && clientUuid != null
                                                ? getCachedClientRoleWithAttributes(clientUuid, child.getName())
                                                : getCachedRoleWithAttributes(child.getName());
                                        if (childDto.getChildren() == null)
                                                childDto.setChildren(new java.util.ArrayList<>());
                                        parentDto.getChildren().add(childDto);
                                        childRoleNames.add(child.getName());
                                }
                        } catch (Exception e) {
                                log.warn("Failed to fetch composites for role: {}", parentDto.getName());
                        }
                }

                // Return only root roles (not children of any other role)
                return dtoMap.values().stream()
                                .filter(dto -> !childRoleNames.contains(dto.getName()))
                                .toList();
        }

        // ==================== Organization API ====================

        /**
         * Membuat Keycloak Organization baru untuk sebuah toko.
         *
         * @param orgName Nama organisation (biasanya nama toko)
         * @return Organization ID dari Keycloak
         * @throws BusinessException Jika gagal membuat organization
         */
        public String createOrganization(String orgName) throws BusinessException {
                OrganizationRepresentation orgRep = new OrganizationRepresentation();
                orgRep.setName(orgName);
                orgRep.setEnabled(true);

                // Slug for alias and domain (no spaces allowed in alias)
                String slug = orgName.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");
                orgRep.setAlias(slug);

                // Keycloak requires at least one domain for Organizations
                OrganizationDomainRepresentation domain = new OrganizationDomainRepresentation();
                domain.setName(slug + ".omnip.local");
                domain.setVerified(false);
                orgRep.addDomain(domain);

                try (Response resp = this.keycloak
                        .realm(this.realm)
                                .organizations()
                                .create(orgRep)) {

                        if (resp.getStatus() != 201) {
                                String body = resp.readEntity(String.class);
                                log.error("Failed to create organization '{}': status={} body={}", orgName, resp.getStatus(),
                                        body);
                                throw new BusinessException("Failed to create Keycloak organization '" + orgName
                                        + "': HTTP " + resp.getStatus() + " - " + body);
                        }

                        String orgId = resp.getLocation()
                                .getPath()
                                .replaceAll(".*/([^/]+)$", "$1");
                        log.info("Organization '{}' created with id='{}'", orgName, orgId);
                        return orgId;
                }
        }

        /**
         * Menambahkan user sebagai member dari suatu Keycloak Organization.
         *
         * @param orgId  Keycloak Organization ID
         * @param userId Keycloak User ID
         * @throws BusinessException Jika gagal menambahkan member
         */
        public void addMemberToOrganization(String orgId, String userId) throws BusinessException {
                try {
                        log.info("Attempting to add user '{}' as member to organization '{}'", userId, orgId);
                        try (Response resp = this.keycloak
                                .realm(this.realm)
                                        .organizations()
                                        .get(orgId)
                                        .members()
                                        .addMember(userId)) {
                                if (resp != null) {
                                        log.info("addMember response: status={}", resp.getStatus());
                                        if (resp.getStatus() >= 400) {
                                                String body = resp.readEntity(String.class);
                                                log.error("Failed to add member: status={} body={}", resp.getStatus(), body);
                                                throw new BusinessException("Failed to add member to organization: " + body);
                                        }
                                }
                        }
                        log.info("User '{}' added as member to organization '{}'", userId, orgId);
                } catch (BusinessException e) {
                        throw e;
                } catch (Exception e) {
                        log.error("Failed to add user '{}' to organization '{}'", userId, orgId, e);
                        throw new BusinessException("Gagal menambahkan member ke organisasi. Silakan coba lagi.");
                }
        }

        /**
         * Membuat Reseller User di Keycloak.
         * User TIDAK diberikan password — Keycloak mengirim email set-password via
         * requiredActions = [UPDATE_PASSWORD].
         *
         * @param username Username (biasanya nama pendek / toko)
         * @param fullname Nama lengkap reseller
         * @param email    Email reseller (akan digunakan sebagai login)
         * @return Keycloak User ID dari reseller yang baru dibuat
         * @throws BusinessException Jika gagal membuat user
         */
        public String createResellerUser(String username, String fullname, String email)
                        throws BusinessException {
                UserRepresentation userRep = this.keycloakAdminClientBusiness
                                .prepareResellerUserRepresentation(username, fullname, email);

                Response resp = this.keycloak.realm(this.realm).users().create(userRep);
                String createdUserId = this.keycloakAdminClientBusiness.extractCreatedUserId(resp);

                // Send email action so user can set their own password
                // Wrapped in try-catch: if Keycloak SMTP is not configured, user is still
                // created
                try {
                        this.keycloak.realm(this.realm)
                                        .users()
                                        .get(createdUserId)
                                        .executeActionsEmail(List.of("UPDATE_PASSWORD"));
                } catch (Exception e) {
                        log.warn("Failed to send UPDATE_PASSWORD email for user '{}'. " +
                                        "SMTP might not be configured in Keycloak. User was still created.", email, e);
                }

                log.info("Reseller user '{}' created: id='{}'", email, createdUserId);
                return createdUserId;
        }

        public boolean userExistsByEmail(String email) {
                List<UserRepresentation> users = this.keycloak.realm(this.realm)
                                .users()
                                .searchByEmail(email, true); // exact match
                return users != null && !users.isEmpty();
        }

        // ==================== Shared DTO Methods ====================
        // These methods return shared DTOs for use by shared layer components
        // to avoid coupling shared layer to domain models.

        /**
         * Get menu roles for a user as shared DTOs.
         * Delegates to getMenuRoles() and converts to RoleInfo.
         *
         * @param userId Keycloak user ID
         * @return List of RoleInfo for sidebar menu display
         */
        public List<RoleInfo> getMenuRoleInfos(String userId) {
                List<KeycloakRole> roles = getMenuRoles(userId);
                return identityMapper.toRoleInfoList(roles);
        }

        /**
         * Get user groups as shared DTOs.
         * Delegates to getUserGroups() and converts to GroupInfo.
         *
         * @param userId Keycloak user ID
         * @return List of GroupInfo
         */
        public List<GroupInfo> getUserGroupInfos(String userId) {
                List<KeycloakGroup> groups = getUserGroups(userId);
                return identityMapper.toGroupInfoList(groups);
        }
}