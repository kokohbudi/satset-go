package com.satset.quickmenu.web;

import com.satset.quickmenu.model.MenuItem;
import com.satset.quickmenu.service.menu.QuickMenuService;
import com.satset.shared.dto.UserDTO;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuickMenuCardControllerTest {

    @Mock
    private QuickMenuService service;
    @Mock
    private HttpSession session;

    private QuickMenuCardController controller;

    @BeforeEach
    void setup() {
        UserDTO userDTO = new UserDTO();
        userDTO.setProviderUserId("u");
        controller = new QuickMenuCardController(service, userDTO);
    }

    @Test
    void card_returnsFragmentWithQuickMenuModel() {
        var item = new MenuItem("users", "/users", "icon-users", "Users");
        when(service.quickMenu(eq("u"), any())).thenReturn(List.of(item));
        Model model = new ExtendedModelMap();

        String view = controller.card(model, session);

        assertThat(view).isEqualTo("components/quick-menu-card :: card");
        assertThat(model.getAttribute("quickMenu")).isEqualTo(List.of(item));
    }
}
