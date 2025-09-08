package com.omnip.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class Token {
    @GetMapping({"/api/token", "/login"})
    public ResponseEntity<Map> getToken(@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        return ResponseEntity.ok(Map.of("accessToken", accessToken));
    }
}
