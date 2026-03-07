package com.satset.shared.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RoleInfoTest {

    @Test
    void shouldCreateRoleInfoWithBuilder() {
        RoleInfo roleInfo = RoleInfo.builder()
                .id("role-123")
                .name("manage_users")
                .description("Can manage users")
                .clientRole(true)
                .composite(false)
                .containerId("client-uuid")
                .attributes(Map.of("sidebar", "1", "url", "/admin/users"))
                .build();

        assertThat(roleInfo.getId()).isEqualTo("role-123");
        assertThat(roleInfo.getName()).isEqualTo("manage_users");
        assertThat(roleInfo.getDescription()).isEqualTo("Can manage users");
        assertThat(roleInfo.getClientRole()).isTrue();
        assertThat(roleInfo.getComposite()).isFalse();
        assertThat(roleInfo.getContainerId()).isEqualTo("client-uuid");
        assertThat(roleInfo.getAttributes()).containsEntry("sidebar", "1");
        assertThat(roleInfo.getAttributes()).containsEntry("url", "/admin/users");
    }

    @Test
    void shouldCreateRoleInfoWithNoArgs() {
        RoleInfo roleInfo = new RoleInfo();
        roleInfo.setId("role-456");
        roleInfo.setName("view_users");

        assertThat(roleInfo.getId()).isEqualTo("role-456");
        assertThat(roleInfo.getName()).isEqualTo("view_users");
    }

    @Test
    void shouldHandleNullChildren() {
        RoleInfo roleInfo = RoleInfo.builder()
                .id("role-789")
                .name("composite_role")
                .composite(true)
                .build();

        // Default children should be empty list
        assertThat(roleInfo.getChildren()).isEmpty();
    }

    @Test
    void shouldSupportNestedChildren() {
        RoleInfo childRole = RoleInfo.builder()
                .id("child-1")
                .name("create_users")
                .build();

        RoleInfo parentRole = RoleInfo.builder()
                .id("parent-1")
                .name("manage_users")
                .composite(true)
                .children(List.of(childRole))
                .build();

        assertThat(parentRole.getChildren()).hasSize(1);
        assertThat(parentRole.getChildren().get(0).getName()).isEqualTo("create_users");
    }

    @Test
    void shouldSupportEqualsAndHashCode() {
        RoleInfo role1 = RoleInfo.builder().id("1").name("admin").build();
        RoleInfo role2 = RoleInfo.builder().id("1").name("admin").build();
        RoleInfo role3 = RoleInfo.builder().id("2").name("user").build();

        assertThat(role1).isEqualTo(role2);
        assertThat(role1).isNotEqualTo(role3);
        assertThat(role1.hashCode()).isEqualTo(role2.hashCode());
    }
}