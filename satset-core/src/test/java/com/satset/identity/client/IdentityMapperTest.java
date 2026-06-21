package com.satset.identity.client;

import com.satset.identity.model.KeycloakRole;
import com.satset.shared.dto.RoleInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityMapperTest {

    private final IdentityMapper mapper = new IdentityMapper();

    // ==================== Role Mapping Tests ====================

    @Test
    void shouldMapKeycloakRoleToRoleInfo() {
        KeycloakRole domainRole = KeycloakRole.builder()
                .id("role-123")
                .name("manage_users")
                .description("Can manage users")
                .clientRole(true)
                .composite(false)
                .containerId("client-uuid")
                .attributes(Map.of("sidebar", "1", "url", "/admin/users"))
                .build();

        RoleInfo result = mapper.toRoleInfo(domainRole);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("role-123");
        assertThat(result.getName()).isEqualTo("manage_users");
        assertThat(result.getDescription()).isEqualTo("Can manage users");
        assertThat(result.getClientRole()).isTrue();
        assertThat(result.getComposite()).isFalse();
        assertThat(result.getContainerId()).isEqualTo("client-uuid");
        assertThat(result.getAttributes()).containsEntry("sidebar", "1");
        assertThat(result.getAttributes()).containsEntry("url", "/admin/users");
    }

    @Test
    void shouldReturnNullForNullRole() {
        RoleInfo result = mapper.toRoleInfo(null);
        assertThat(result).isNull();
    }

    @Test
    void shouldMapRoleListToRoleInfoList() {
        KeycloakRole role1 = KeycloakRole.builder()
                .id("1")
                .name("admin")
                .build();
        KeycloakRole role2 = KeycloakRole.builder()
                .id("2")
                .name("user")
                .build();

        List<RoleInfo> result = mapper.toRoleInfoList(List.of(role1, role2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("admin");
        assertThat(result.get(1).getName()).isEqualTo("user");
    }

    @Test
    void shouldReturnEmptyListForNullRoleList() {
        List<RoleInfo> result = mapper.toRoleInfoList(null);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldMapCompositeRoleWithChildren() {
        KeycloakRole childRole = KeycloakRole.builder()
                .id("child-1")
                .name("create_users")
                .build();

        KeycloakRole parentRole = KeycloakRole.builder()
                .id("parent-1")
                .name("manage_users")
                .composite(true)
                .children(List.of(childRole))
                .build();

        RoleInfo result = mapper.toRoleInfo(parentRole);

        assertThat(result.getComposite()).isTrue();
        assertThat(result.getChildren()).hasSize(1);
        assertThat(result.getChildren().get(0).getName()).isEqualTo("create_users");
    }
}