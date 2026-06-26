package com.satset.quickmenu.web;

import com.satset.quickmenu.service.QuickMenuService;
import com.satset.shared.dto.UserDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Toggles a user's dashboard quick-menu pin. CSRF handled globally (layouts/base.html). */
@RestController
@RequestMapping("/quick-menu")
public class QuickMenuController {

    private final QuickMenuService service;
    private final UserDTO userDTO;

    public QuickMenuController(QuickMenuService service, UserDTO userDTO) {
        this.service = service;
        this.userDTO = userDTO;
    }

    @PostMapping("/toggle")
    public ResponseEntity<ToggleResponse> toggle(@RequestBody ToggleRequest req) {
        String roleName = req.roleName();
        if (roleName == null || roleName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean pinned = service.toggle(userDTO.getProviderUserId(), roleName);
        return ResponseEntity.ok(new ToggleResponse(pinned));
    }

    public record ToggleRequest(String roleName) {
    }

    public record ToggleResponse(boolean pinned) {
    }
}
