package com.satset.quickmenu.web;

import com.satset.quickmenu.service.QuickMenuService;
import com.satset.shared.dto.RoleInfo;
import com.satset.shared.dto.UserDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Renders the dashboard "Menu Cepat" card fragment on demand so the sidebar
 * pin toggle can live-sync the card without a full page reload.
 */
@Controller
public class QuickMenuCardController {

    private final QuickMenuService service;
    private final UserDTO userDTO;

    public QuickMenuCardController(QuickMenuService service, UserDTO userDTO) {
        this.service = service;
        this.userDTO = userDTO;
    }

    @GetMapping("/quick-menu/card")
    public String card(Model model, HttpSession session) {
        @SuppressWarnings("unchecked")
        List<RoleInfo> userRoles = (List<RoleInfo>) session.getAttribute("userRoles");
        model.addAttribute("quickMenu", service.quickMenu(userDTO.getProviderUserId(), userRoles));
        return "components/quick-menu-card :: card";
    }
}
