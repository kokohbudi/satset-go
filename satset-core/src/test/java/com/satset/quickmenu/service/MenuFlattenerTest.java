package com.satset.quickmenu.service;

import com.satset.quickmenu.model.MenuItem;
import com.satset.shared.dto.RoleInfo;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MenuFlattenerTest {

    private final MenuFlattener flattener = new MenuFlattener();

    private RoleInfo role(String name, Map<String, String> attrs, List<RoleInfo> children) {
        return RoleInfo.builder()
                .name(name)
                .attributes(attrs)
                .children(children == null ? List.of() : children)
                .build();
    }

    private Map<String, String> menuAttrs(String url, String icon, String displayName) {
        Map<String, String> m = new HashMap<>();
        m.put("sidebar", "1");
        m.put("url", url);
        if (icon != null) m.put("icon", icon);
        if (displayName != null) m.put("display_name", displayName);
        return m;
    }

    @Test
    void flattensStandaloneSidebarRole() {
        var roles = List.of(role("users", menuAttrs("/users", "icon-users", "Users"), null));
        var out = flattener.flatten(roles);
        assertThat(out).singleElement().satisfies(i -> {
            assertThat(i.name()).isEqualTo("users");
            assertThat(i.url()).isEqualTo("/users");
            assertThat(i.icon()).isEqualTo("icon-users");
            assertThat(i.displayName()).isEqualTo("Users");
        });
    }

    @Test
    void flattensChildrenAndSkipsParent() {
        var child = role("view_catalog", menuAttrs("/catalog", null, null), null);
        var parent = role("catalog_section", Map.of("sidebar", "1"), List.of(child));
        var out = flattener.flatten(List.of(parent));
        assertThat(out).extracting(MenuItem::name).containsExactly("view_catalog");
    }

    @Test
    void skipsNonSidebarRoles() {
        var roles = List.of(role("hidden", Map.of("sidebar", "0", "url", "/x"), null));
        assertThat(flattener.flatten(roles)).isEmpty();
    }

    @Test
    void defaultsIconAndDisplayName() {
        var roles = List.of(role("deposit", menuAttrs("/deposit", null, null), null));
        var item = flattener.flatten(roles).get(0);
        assertThat(item.icon()).isEqualTo("icon-document");
        assertThat(item.displayName()).isEqualTo("deposit");
    }

    @Test
    void nullSafe() {
        assertThat(flattener.flatten(null)).isEmpty();
    }
}
