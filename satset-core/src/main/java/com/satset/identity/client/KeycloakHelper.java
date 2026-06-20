package com.satset.identity.client;

import com.satset.shared.exception.BusinessException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class KeycloakHelper {

    /**
     * Mempersiapkan representasi user Keycloak untuk pembuatan baru
     *
     * @param username Username untuk user
     * @param fullname Nama lengkap user
     * @param email    Email user
     * @return UserRepresentation yang siap untuk dibuat di Keycloak
     */
    public UserRepresentation prepareUserRepresentation(String username, String fullname, String email) {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(email); // Business rule: username = email
        userRep.setEmail(email);
        userRep.setEnabled(true); // Business rule: user baru selalu aktif
        userRep.setFirstName(username);
        userRep.setLastName(username); // Business rule: first/last name sama

        return userRep;
    }

    /**
     * Ekstrak user ID dari response setelah pembuatan user
     *
     * @param response HTTP Response dari Keycloak API
     * @return User ID dari path location
     * @throws BusinessException Jika gagal mengekstrak ID
     */
    public String extractCreatedUserId(Response response) throws BusinessException {
        if (response.getStatus() != 201) {
            Map<String, Object> error = response.readEntity(new GenericType<>() {
            });
            log.error(error.toString());
            String errorMessage = error.get("errorMessage") != null ? error.get("errorMessage").toString()
                    : "Error create user";
            throw new BusinessException(errorMessage);
        }

        // Business logic untuk ekstrak ID dari response path
        return response.getLocation()
                .getPath()
                .replaceAll(".*/([^/]+)$", "$1");
    }

    /**
     * Mempersiapkan kredensial password untuk user
     *
     * @param password    Password yang akan diset
     * @param isTemporary Apakah password bersifat sementara
     * @return CredentialRepresentation
     */
    public CredentialRepresentation preparePasswordCredential(String password, boolean isTemporary) {
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);
        cred.setTemporary(isTemporary); // Business rule: default temporary = true

        return cred;
    }

    /**
     * Mempersiapkan representasi user untuk update password
     *
     * @param password Password baru
     * @return UserRepresentation dengan credential yang sudah diset
     */
    public UserRepresentation preparePasswordUpdateRepresentation(String password) {
        UserRepresentation userRep = new UserRepresentation();
        CredentialRepresentation cred = this.preparePasswordCredential(password, false);
        userRep.setCredentials(List.of(cred));

        return userRep;
    }

    /**
     * Mempersiapkan representasi user untuk update status
     *
     * @param isEnabled Status baru (aktif/nonaktif)
     * @return UserRepresentation dengan enabled status yang sudah diset
     */
    public UserRepresentation prepareStatusUpdateRepresentation(boolean isEnabled) {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setEnabled(isEnabled);

        return userRep;
    }

    /**
     * Mempersiapkan representasi reseller user dengan UPDATE_PASSWORD required
     * action.
     * Keycloak akan mengirim email untuk user agar set password sendiri.
     *
     * @param username Username (biasanya email)
     * @param fullname Nama lengkap reseller
     * @param email    Email reseller
     * @return UserRepresentation dengan requiredActions = [UPDATE_PASSWORD]
     */
    public UserRepresentation prepareResellerUserRepresentation(String username, String fullname, String email) {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(email);
        userRep.setEmail(email);
        userRep.setEnabled(true);
        userRep.setEmailVerified(true);
        userRep.setFirstName(username);
        userRep.setLastName(fullname);
        userRep.setRequiredActions(List.of("UPDATE_PASSWORD"));
        return userRep;
    }
}