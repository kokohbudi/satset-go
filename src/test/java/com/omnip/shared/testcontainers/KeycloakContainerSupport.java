package com.omnip.shared.testcontainers;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

/**
 * Singleton Keycloak container — starts once per JVM, shared across all IT classes.
 * Realm setup done programmatically via Admin API (version-agnostic, no JSON format issues).
 */
public abstract class KeycloakContainerSupport {

    protected static final String TEST_REALM = "satset-go";
    protected static final String TEST_CLIENT_ID = "satsetgo-client";
    protected static final String TEST_CLIENT_SECRET = "test-client-secret";
    protected static final KeycloakContainer KEYCLOAK;

    static {
        configureDockerHost();
        KEYCLOAK = new KeycloakContainer("quay.io/keycloak/keycloak:26.0.5");
        KEYCLOAK.start();
        setupTestRealm();
    }

    private static void configureDockerHost() {
        if (System.getenv("DOCKER_HOST") != null && !System.getenv("DOCKER_HOST").isEmpty()) {
            return;
        }
        String[] candidates = {
            System.getProperty("user.home") + "/.docker/run/docker.sock",
            "/var/run/docker.sock"
        };
        for (String path : candidates) {
            if (new java.io.File(path).exists()) {
                System.setProperty("DOCKER_HOST", "unix://" + path);
                break;
            }
        }
    }

    private static void setupTestRealm() {
        try (Keycloak admin = masterAdminClient()) {
            // 1. Create realm
            RealmRepresentation realm = new RealmRepresentation();
            realm.setRealm(TEST_REALM);
            realm.setEnabled(true);
            realm.setSslRequired("none");
            realm.setLoginWithEmailAllowed(true);
            realm.setDuplicateEmailsAllowed(false);
            admin.realms().create(realm);

            // 2. Create realm roles (mirrors production satset-go roles)
            for (String roleName : List.of("view_users", "manage_users", "view_catalog", "manage_catalog")) {
                RoleRepresentation role = new RoleRepresentation();
                role.setName(roleName);
                role.setComposite(false);
                admin.realm(TEST_REALM).roles().create(role);
            }

            // 3. Create client
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(TEST_CLIENT_ID);
            client.setEnabled(true);
            client.setPublicClient(false);
            client.setSecret(TEST_CLIENT_SECRET);
            client.setDirectAccessGrantsEnabled(true);
            client.setServiceAccountsEnabled(true);
            client.setStandardFlowEnabled(false);
            client.setProtocol("openid-connect");
            admin.realm(TEST_REALM).clients().create(client);

            // 4. Create seed user
            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setValue("password");
            cred.setTemporary(false);

            UserRepresentation user = new UserRepresentation();
            user.setUsername("testuser");
            user.setEmail("testuser@example.com");
            user.setFirstName("Test");
            user.setLastName("User");
            user.setEnabled(true);
            user.setEmailVerified(true);
            user.setCredentials(List.of(cred));
            admin.realm(TEST_REALM).users().create(user);
        }
    }

    protected static Keycloak masterAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(KEYCLOAK.getAuthServerUrl())
                .realm("master")
                .clientId("admin-cli")
                .username(KEYCLOAK.getAdminUsername())
                .password(KEYCLOAK.getAdminPassword())
                .build();
    }

    protected static Keycloak testRealmAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(KEYCLOAK.getAuthServerUrl())
                .realm("master")
                .clientId("admin-cli")
                .username(KEYCLOAK.getAdminUsername())
                .password(KEYCLOAK.getAdminPassword())
                .build();
    }
}
