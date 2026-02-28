package com.omnip.shared.constant;

public class OmniConstants {
    public static final String REGISTRATION_CHANNEL_KEYCLOAK = "keycloak";
    public static final String SESSION_USER_DTO = "SESSION_userDTO";

    // Security role prefixes (maps Keycloak roles to Spring Security GrantedAuthority)
    public static final String ROLE_PREFIX_REALM = "ROLE_REALM_";
    public static final String ROLE_PREFIX_CLIENT = "ROLE_CLIENT_";

    // Realm role names for @PreAuthorize (Spring strips "ROLE_" prefix in hasRole())
    public static final String PERM_VIEW_USERS = "REALM_view_users";
    public static final String PERM_MANAGE_USERS = "REALM_manage_users";
    public static final String PERM_CREATE_USERS = "REALM_create_users";
    public static final String PERM_MANAGE_ROLES = "REALM_manage_roles";
    public static final String PERM_ASSIGN_USER_TO_GROUPS = "REALM_assign_user_to_groups";
    public static final String PERM_VIEW_RESELLERS = "REALM_view-resellers";
    public static final String PERM_CREATE_RESELLER = "REALM_create-reseller";
}
