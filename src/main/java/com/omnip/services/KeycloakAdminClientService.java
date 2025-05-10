package com.omnip.services;

import com.omnip.BusinessException;
import com.omnip.dto.UserDTO;
import com.omnip.entities.Users;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KeycloakAdminClientService {
    private final Keycloak keycloak;
    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    private final UserDTO userDTO;

    private final UserService userService;

    public KeycloakAdminClientService(Keycloak keycloak, UserDTO userDTO, UserService userService) {
        this.keycloak = keycloak;
        this.userDTO = userDTO;
        this.userService = userService;
    }


    public Users updateUser(String providerUserId, UserRepresentation userRepresentation) {
        UsersResource usersResource = this.keycloak.realm(this.realm).users();
        usersResource.get(providerUserId).update(userRepresentation);
        return this.userService.findByProviderUserId(providerUserId);
    }


    public Users changeUserPassword(String userId, String newPassword) {
        UserRepresentation userRep = new UserRepresentation();

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(newPassword);
        cred.setTemporary(false);

        userRep.setCredentials(List.of(cred));

        // Optional: update other fields if needed
        // userRep.setEnabled(true);

        return this.updateUser(userId, userRep);
    }

    public UserDTO createUser(String username, String fullname, String email, String password, String requestedRole) throws BusinessException {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(email);
        userRep.setEmail(email);
        userRep.setEnabled(true);
        userRep.setFirstName(username);
        userRep.setLastName(username);

        Response resp = this.keycloak.realm(this.realm).users().create(userRep);
        if (resp.getStatus() != 201) {
            Map error = resp.readEntity(new GenericType<Map<String, Object>>() {
            });
            log.error(error.toString());
            throw new BusinessException(error.get("errorMessage") != null ? error.get("errorMessage").toString() : "Error create user");
        }

        String userId = resp.getLocation()
                .getPath()
                .replaceAll(".*/([^/]+)$", "$1");

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);
        cred.setTemporary(true);
        this.keycloak.realm(this.realm)
                .users()
                .get(userId)
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

        // 5. Assign role ke user (client‐level role)
        this.keycloak.realm(this.realm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(clientRes.toRepresentation().getId())
                .add(List.of(role));

        UserDTO userDTO = new UserDTO();
        userDTO.setProviderUserId(userId);
        userDTO.setUsername(username);
        userDTO.setEmail(email);
        userDTO.setFullname(fullname);
        userDTO.setRoles(List.of(requestedRole));
        return userDTO;
    }

}