package com.satset.shared.testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;

/**
 * Singleton Keycloak container — starts once per JVM, shared across all IT classes.
 * Realm setup via JSON import (satset-go-realm-full.json exported from live KC 26.2.1).
 * Includes all roles, users, and clients from production realm.
 *
 * Note: KC 26.1+ sets sslRequired=EXTERNAL on master realm even in start-dev mode.
 * We use kcadm.sh inside the container to disable SSL before calling admin API externally.
 */
public abstract class KeycloakContainerSupport {

    protected static final String TEST_REALM = "satset-go";
    protected static final String TEST_CLIENT_ID = "satsetgo-client";
    protected static final String TEST_CLIENT_SECRET = "test-client-secret";
    protected static final KeycloakContainer KEYCLOAK;

    static {
        configureDockerHost();
        KEYCLOAK = new KeycloakContainer("quay.io/keycloak/keycloak:26.2.1");
        KEYCLOAK.start();
        disableSslRequirement();
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

    /**
     * KC 26.1+ sets sslRequired=EXTERNAL on master realm even in start-dev mode.
     * Use kcadm.sh inside the container (localhost → no SSL check) to disable it.
     */
    private static void disableSslRequirement() {
        try {
            KEYCLOAK.execInContainer(
                    "/opt/keycloak/bin/kcadm.sh", "config", "credentials",
                    "--server", "http://localhost:8080",
                    "--realm", "master",
                    "--user", KEYCLOAK.getAdminUsername(),
                    "--password", KEYCLOAK.getAdminPassword()
            );
            KEYCLOAK.execInContainer(
                    "/opt/keycloak/bin/kcadm.sh", "update", "realms/master",
                    "-s", "sslRequired=NONE"
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to disable SSL requirement on master realm", e);
        }
    }

    private static void setupTestRealm() {
        try (Keycloak admin = KEYCLOAK.getKeycloakAdminClient()) {
            // 1. Import realm from JSON export (includes roles, users, clients from live KC)
            RealmRepresentation realm = loadRealmFromJson();
            admin.realms().create(realm);

            // 2. Create testuser with password for credential testing
            // (JSON import doesn't properly set credentials, so we do it programmatically)
            createTestUser(admin);
        } catch (IOException e) {
            throw new RuntimeException("Failed to setup test realm from JSON export", e);
        }
    }

    private static void createTestUser(Keycloak admin) {
        // Create user WITHOUT credentials first
        UserRepresentation user = new UserRepresentation();
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        user.setEmailVerified(true);

        // Save user
        var response = admin.realm(TEST_REALM).users().create(user);
        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        // Set password using password reset API (more reliable than credentials in user creation)
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue("password");

        admin.realm(TEST_REALM).users().get(userId).resetPassword(passwordCred);
    }

    private static RealmRepresentation loadRealmFromJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        // Load from classpath (test resources)
        ClassLoader classLoader = KeycloakContainerSupport.class.getClassLoader();
        String json = new String(classLoader.getResourceAsStream("satset-go-realm-full.json").readAllBytes());
        return mapper.readValue(json, RealmRepresentation.class);
    }

    protected static Keycloak masterAdminClient() {
        return KEYCLOAK.getKeycloakAdminClient();
    }

    protected static Keycloak testRealmAdminClient() {
        return KEYCLOAK.getKeycloakAdminClient();
    }
}
