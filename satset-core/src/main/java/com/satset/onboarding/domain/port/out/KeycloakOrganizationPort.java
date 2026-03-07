package com.satset.onboarding.domain.port.out;

import com.satset.shared.exception.BusinessException;

/**
 * Output port for Keycloak organization operations.
 * Used by onboarding services to create organizations and manage membership.
 */
public interface KeycloakOrganizationPort {

    String createOrganization(String orgName) throws BusinessException;

    void addMemberToOrganization(String orgId, String userId) throws BusinessException;

    String createResellerUser(String username, String fullname, String email) throws BusinessException;

    void assignClientRoleToUser(String userId, String roleName) throws BusinessException;

    boolean userExistsByEmail(String email);
}
