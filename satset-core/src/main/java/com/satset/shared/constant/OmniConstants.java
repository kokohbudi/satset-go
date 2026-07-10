package com.satset.shared.constant;

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

    // Admin Catalog Management
    public static final String PERM_VIEW_CATALOG = "REALM_view_catalog";
    public static final String PERM_MANAGE_CATEGORIES = "REALM_manage_categories";
    public static final String PERM_MANAGE_PRODUCTS = "REALM_manage_products";
    public static final String PERM_MANAGE_DENOMS = "REALM_manage_denoms";

    // Wallet admin
    public static final String PERM_ADJUST_BALANCE = "REALM_adjust_balance";

    // Accounting / reports
    public static final String PERM_VIEW_REPORTS = "REALM_view_reports";

    // Client roles (satsetgo-client) — mapped to ROLE_CLIENT_<role> by JwtAuthenticationConverter
    public static final String PERM_TRANSACTION = "CLIENT_transaction"; // view transaksi + balance
    public static final String PERM_PURCHASE = "CLIENT_purchase"; // lakukan pembelian + browse katalog
}
