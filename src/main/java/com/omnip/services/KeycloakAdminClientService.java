package com.omnip.services;

import com.omnip.business.KeycloakAdminClientBusiness;
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

    public void updateUser(String providerUserId, UserRepresentation userRepresentation) {
        UsersResource usersResource = this.keycloak.realm(this.realm).users();
        usersResource.get(providerUserId).update(userRepresentation);
    }

    public void changeUserPassword(String userId, String newPassword) {
        UserRepresentation userRep = this.keycloakAdminClientBusiness.preparePasswordUpdateRepresentation(newPassword);
        this.updateUser(userId, userRep);
    }

    public String createUser(String username, String fullname, String email, String password, String requestedRole) throws BusinessException {
        UserRepresentation userRep = this.keycloakAdminClientBusiness.prepareUserRepresentation(username, fullname, email);
        Response resp = this.keycloak.realm(this.realm).users().create(userRep);
        String createdUserId = this.keycloakAdminClientBusiness.extractCreatedUserId(resp);
        CredentialRepresentation cred = this.keycloakAdminClientBusiness.preparePasswordCredential(password, true);
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
        UserRepresentation userRep = this.keycloakAdminClientBusiness.prepareStatusUpdateRepresentation(isEnabled);
        this.updateUser(userId, userRep);
    }
}