package com.omnip.services;

import jakarta.annotation.PostConstruct;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeycloakAdminClientService {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private Keycloak keycloak;

    @PostConstruct
    public void init() {
//        this.keycloak = Keycloak.getInstance(
//                this.serverUrl,
//                this.realm,
//                "sibebek", // username admin
//                "kozaninja", // password admin
//                this.clientId,
//                this.clientSecret
//        );

        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(this.serverUrl)       // misal http://localhost:8888
                .realm(this.realm)               // misal "master"
                .clientId(this.clientId)         // "omnip-client" atau "omnip-admin-client"
                .clientSecret(this.clientSecret)
                .username("sibebek")
                .grantType("password")
                .password("kozaninja")// secret yang kamu lihat di tab Credentials
                .build();
    }

    public void updateUser(String userId, UserRepresentation userRepresentation) {
        UsersResource usersResource = this.keycloak.realm(this.realm).users();
        usersResource.get(userId).update(userRepresentation);
    }
    

    public void changeUserPassword(String userId, String newPassword) {
        UserRepresentation userRep = new UserRepresentation();

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(newPassword);
        cred.setTemporary(false);

        userRep.setCredentials(List.of(cred));

        // Optional: update other fields if needed
        // userRep.setEnabled(true);

        this.updateUser(userId, userRep);
    }

}