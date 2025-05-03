package com.omnip.controllers;

import com.omnip.dto.UserDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AccountController {
    private final JwtDecoder jwtDecoder;
    private final UserDTO userDTO;

    public AccountController(JwtDecoder jwtDecoder, UserDTO userDTO) {
        this.jwtDecoder = jwtDecoder;
        this.userDTO = userDTO;
    }
    @GetMapping("/api/account")
    public UserDTO account() {
        return this.userDTO;
    }
    public Map<String,Object> me(Authentication auth,
                                 @AuthenticationPrincipal OidcUser oidcUser) {
        System.out.println("Auth principal: " + auth.getPrincipal());
        return Map.of("oidcUser", oidcUser);
    }

    @GetMapping("/api/jos")
    public String getUserInfo(@AuthenticationPrincipal Jwt jwt, HttpSession session) {
        // Cek apakah JWT ada di header atau session
        if (jwt != null) {
            return "Hello " + jwt.getClaim("name") + " (from JWT)";
        } else {
            String token = (String) session.getAttribute("access_token");
            return token != null ? "Hello (from session)" : "No token found";
        }
    }

    @GetMapping("/api/roles")
    public Map debugRoles(@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String token   = client.getAccessToken().getTokenValue();
        Jwt    jwt     = this.jwtDecoder.decode(token);

        // Gabungkan header + claims ke dalam satu map
        Map<String,Object> result = new HashMap<>();
        result.put("tokenValue", token);
        result.put("headers",    jwt.getHeaders());
        result.put("claims",     jwt.getClaims().get("resource_access"));
        return result;
    }
}
