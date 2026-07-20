package com.satset.webhook.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * This deploy has exactly one public route. No Keycloak/OAuth2 — the webhook
 * path is authenticated by {@code DigiflazzSignatureVerifier}, not a login.
 * Everything else denied by default, same protection as core even though this
 * app runs on the public internet.
 */
@Configuration
@EnableWebSecurity
public class WebhookSecurityConfig {

    @Bean
    public SecurityFilterChain webhookFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/webhooks/digiflazz").permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }
}
