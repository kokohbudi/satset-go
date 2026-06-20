package com.satset.identity.client;

import com.satset.identity.model.KeycloakGroup;
import com.satset.identity.model.KeycloakRole;
import com.satset.shared.dto.GroupInfo;
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

    // ==================== Group Mapping Tests ====================

    @Test
    void shouldMapKeycloakGroupToGroupInfo() {
        KeycloakGroup domainGroup = KeycloakGroup.builder()
                .id("group-123")
                .name("admin")
                .path("/backoffice/admin")
                .parentId("parent-456")
                .build();

        GroupInfo result = mapper.toGroupInfo(domainGroup);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("group-123");
        assertThat(result.getName()).isEqualTo("admin");
        assertThat(result.getPath()).isEqualTo("/backoffice/admin");
        assertThat(result.getParentId()).isEqualTo("parent-456");
    }

    @Test
    void shouldReturnNullForNullGroup() {
        GroupInfo result = mapper.toGroupInfo(null);
        assertThat(result).isNull();
    }

    @Test
    void shouldMapGroupListToGroupInfoList() {
        KeycloakGroup group1 = KeycloakGroup.builder()
                .id("1")
                .name("admin")
                .build();
        KeycloakGroup group2 = KeycloakGroup.builder()
                .id("2")
                .name("operator")
                .build();

        List<GroupInfo> result = mapper.toGroupInfoList(List.of(group1, group2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("admin");
        assertThat(result.get(1).getName()).isEqualTo("operator");
    }

    @Test
    void shouldReturnEmptyListForNullGroupList() {
        List<GroupInfo> result = mapper.toGroupInfoList(null);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldMapGroupWithSubGroups() {
        KeycloakGroup subGroup = KeycloakGroup.builder()
                .id("sub-1")
                .name("sub-admin")
                .path("/backoffice/admin/sub-admin")
                .build();

        KeycloakGroup parentGroup = KeycloakGroup.builder()
                .id("parent-1")
                .name("admin")
                .path("/backoffice/admin")
                .subGroups(List.of(subGroup))
                .build();

        GroupInfo result = mapper.toGroupInfo(parentGroup);

        assertThat(result.getSubGroups()).hasSize(1);
        assertThat(result.getSubGroups().get(0).getName()).isEqualTo("sub-admin");
    }
}