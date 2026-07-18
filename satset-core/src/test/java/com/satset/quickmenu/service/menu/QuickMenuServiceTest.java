package com.satset.quickmenu.service.menu;

import com.satset.quickmenu.model.MenuItem;
import com.satset.quickmenu.model.PinnedMenu;
import com.satset.quickmenu.repository.PinnedMenuRepository;
import com.satset.shared.dto.RoleInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuickMenuServiceTest {

    @Mock
    private PinnedMenuRepository repo;

    private QuickMenuService service;
    private List<RoleInfo> userRoles;

    @BeforeEach
    void setup() {
        service = new QuickMenuService(repo, new MenuFlattener());
        userRoles = List.of(RoleInfo.builder()
                .name("users")
                .attributes(Map.of("sidebar", "1", "url", "/users"))
                .children(List.of())
                .build());
    }

    private PinnedMenu pin(String roleName) {
        PinnedMenu p = new PinnedMenu();
        p.setRoleName(roleName);
        return p;
    }

    @Test
    void toggle_pinsWhenNotPinned() {
        when(repo.existsByUserIdAndRoleName("u", "users")).thenReturn(false);

        boolean result = service.toggle("u", "users");

        assertThat(result).isTrue();
        ArgumentCaptor<PinnedMenu> cap = ArgumentCaptor.forClass(PinnedMenu.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getRoleName()).isEqualTo("users");
        assertThat(cap.getValue().getUserId()).isEqualTo("u");
    }

    @Test
    void toggle_unpinsWhenPinned() {
        when(repo.existsByUserIdAndRoleName("u", "users")).thenReturn(true);

        boolean result = service.toggle("u", "users");

        assertThat(result).isFalse();
        verify(repo).deleteByUserIdAndRoleName("u", "users");
        verify(repo, never()).save(any());
    }

    @Test
    void quickMenu_excludesLostRole() {
        when(repo.findByUserId("u"))
                .thenReturn(List.of(pin("users"), pin("ghost")));

        var out = service.quickMenu("u", userRoles);

        assertThat(out).extracting(MenuItem::name).containsExactly("users");
    }

    @Test
    void pinnedRoleNames_returnsNames() {
        when(repo.findByUserId("u")).thenReturn(List.of(pin("users")));

        assertThat(service.pinnedRoleNames("u")).containsExactly("users");
    }
}
