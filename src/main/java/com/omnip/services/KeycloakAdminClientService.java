package com.omnip.services;

import com.omnip.business.KeycloakAdminClientBusiness;
import com.omnip.dtos.KeycloakGroupDTO;
import com.omnip.dtos.KeycloakRoleDTO;
import com.omnip.exceptions.BusinessException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
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