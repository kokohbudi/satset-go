package com.satset.shared.config;

import com.satset.shared.constant.OmniConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig {

    @Value("${keycloak.client-id}")
    private String clientId;

    /**
     * Extract Keycloak realm + client roles from a token's claim map as granted authorities.
     * Realm roles come from {@code realm_access.roles}; client roles from
     * {@code resource_access.<clientId>.roles}. Prefixes come from {@link OmniConstants}.
     */
    private static Set<GrantedAuthority> keycloakRoles(Map<String, Object> claims, String clientId) {
        Set<GrantedAuthority> auths = new HashSet<>();

        Object realmAccess = claims.get("realm_access");
        if (realmAccess instanceof Map<?, ?> realmMap) {
            Object realmRoles = realmMap.get("roles");
            if (realmRoles instanceof List<?> rolesList) {
                rolesList.stream()
                        .map(Object::toString)
                        .map(r -> OmniConstants.ROLE_PREFIX_REALM + r)
                        .map(SimpleGrantedAuthority::new)
                        .forEach(auths::add);
            }
        }

        Object resourceAccess = claims.get("resource_access");
        if (resourceAccess instanceof Map<?, ?> raMap) {
            Object clientAccess = raMap.get(clientId);
            if (clientAccess instanceof Map<?, ?> clientMap) {
                Object clientRoles = clientMap.get("roles");
                if (clientRoles instanceof List<?> clientRolesList) {
                    clientRolesList.stream()
                            .map(Object::toString)
                            .map(r -> OmniConstants.ROLE_PREFIX_CLIENT + r)
                            .map(SimpleGrantedAuthority::new)
                            .forEach(auths::add);
                }
            }
        }

        return auths;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(JwtAuthenticationConverter jwtAuthenticationConverter,
            HttpSecurity http, LogoutSuccessHandler logoutSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - only truly public resources
                        .requestMatchers("/", "/login", "/logout", "/error", "/webjars/**", "/css/**", "/js/**",
                                "/images/**")
                        .permitAll()

                        // Everything else needs a login; fine-grained roles are enforced
                        // per-endpoint via @PreAuthorize (method security).
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/keycloak")))
                .oauth2Login(oauth2 -> oauth2
                        // Selalu ke /dashboard setelah login; abaikan saved-request
                        // agar bookmark/deep-link basi tidak mengembalikan 404.
                        .defaultSuccessUrl("/dashboard", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(userAuthoritiesMapper())))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)))
                // CSRF protected for browser session endpoints; JS sends X-XSRF-TOKEN
                // (see layouts/base.html). Only the logout endpoint is exempt.
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/logout", "/api/logout"))
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "SESSION")
                        .logoutSuccessHandler(logoutSuccessHandler));
        return http.build();
    }

    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clients) {
        OidcClientInitiatedLogoutSuccessHandler handler = new OidcClientInitiatedLogoutSuccessHandler(clients);
        // Setelah logout di Keycloak, redirect kembali ke home page menggunakan
        // placeholder dinamis
        handler.setPostLogoutRedirectUri("{baseUrl}/");
        return handler;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // 1) converter default untuk scopes ("scope" -> SCOPE_…)
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        scopeConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter authConverter = new JwtAuthenticationConverter();
        authConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> auths = new HashSet<>();
            auths.addAll(scopeConverter.convert(jwt));
            auths.addAll(keycloakRoles(jwt.getClaims(), clientId));
            return auths;
        });
        return authConverter;
    }

    @Bean
    public org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                if (authority instanceof org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority oidcAuth) {
                    mappedAuthorities.addAll(keycloakRoles(oidcAuth.getIdToken().getClaims(), clientId));
                }
                mappedAuthorities.add(authority);
            });

            return mappedAuthorities;
        };
    }

}
