package com.satset.quickmenu.web;

import com.satset.quickmenu.service.QuickMenuService;
import com.satset.shared.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuickMenuControllerTest {

    @Mock
    private QuickMenuService service;

    private QuickMenuController controller;

    @BeforeEach
    void setup() {
        UserDTO userDTO = new UserDTO();
        userDTO.setProviderUserId("u");
        controller = new QuickMenuController(service, userDTO);
    }

    @Test
    void toggle_returnsPinnedTrue() {
        when(service.toggle("u", "users")).thenReturn(true);

        var resp = controller.toggle(new QuickMenuController.ToggleRequest("users"));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().pinned()).isTrue();
    }

    @Test
    void toggle_blankRoleNameIsBadRequest() {
        var resp = controller.toggle(new QuickMenuController.ToggleRequest("  "));
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }
}
