package com.omnip.shared.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroupInfoTest {

    @Test
    void shouldCreateGroupInfoWithBuilder() {
        GroupInfo groupInfo = GroupInfo.builder()
                .id("group-123")
                .name("admin")
                .path("/backoffice/admin")
                .parentId("parent-456")
                .build();

        assertThat(groupInfo.getId()).isEqualTo("group-123");
        assertThat(groupInfo.getName()).isEqualTo("admin");
        assertThat(groupInfo.getPath()).isEqualTo("/backoffice/admin");
        assertThat(groupInfo.getParentId()).isEqualTo("parent-456");
    }

    @Test
    void shouldCreateGroupInfoWithNoArgs() {
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setId("group-789");
        groupInfo.setName("operator");

        assertThat(groupInfo.getId()).isEqualTo("group-789");
        assertThat(groupInfo.getName()).isEqualTo("operator");
    }

    @Test
    void shouldHandleNullSubGroups() {
        GroupInfo groupInfo = GroupInfo.builder()
                .id("group-001")
                .name("top-level")
                .build();

        // Default subGroups should be empty list
        assertThat(groupInfo.getSubGroups()).isEmpty();
    }

    @Test
    void shouldSupportNestedSubGroups() {
        GroupInfo subGroup = GroupInfo.builder()
                .id("sub-1")
                .name("sub-admin")
                .path("/backoffice/admin/sub-admin")
                .build();

        GroupInfo parentGroup = GroupInfo.builder()
                .id("parent-1")
                .name("admin")
                .path("/backoffice/admin")
                .subGroups(List.of(subGroup))
                .build();

        assertThat(parentGroup.getSubGroups()).hasSize(1);
        assertThat(parentGroup.getSubGroups().get(0).getName()).isEqualTo("sub-admin");
    }

    @Test
    void shouldSupportTopLevelGroupWithNullParentId() {
        GroupInfo topLevelGroup = GroupInfo.builder()
                .id("root-1")
                .name("backoffice")
                .path("/backoffice")
                .parentId(null)
                .build();

        assertThat(topLevelGroup.getParentId()).isNull();
    }

    @Test
    void shouldSupportEqualsAndHashCode() {
        GroupInfo group1 = GroupInfo.builder().id("1").name("admin").build();
        GroupInfo group2 = GroupInfo.builder().id("1").name("admin").build();
        GroupInfo group3 = GroupInfo.builder().id("2").name("user").build();

        assertThat(group1).isEqualTo(group2);
        assertThat(group1).isNotEqualTo(group3);
        assertThat(group1.hashCode()).isEqualTo(group2.hashCode());
    }
}