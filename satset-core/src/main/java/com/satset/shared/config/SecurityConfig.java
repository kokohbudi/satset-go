package com.satset.shared.config;

import com.satset.shared.constant.OmniConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
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
@EnableCaching
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig {

    @Value("${keycloak.client-id}")
    private String clientId;

    public SecurityConfig() {
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

                        // Role management endpoints - require admin role
                        .requestMatchers("/admin/roles/**").authenticated()
                        .requestMatchers("/api/roles/**").authenticated()

                        // User management endpoints - require authenticated with specific roles
                        .requestMatchers("/api/users/**").authenticated()

                        // Admin catalog management
                        .requestMatchers("/admin/catalog/**").authenticated()
                        .requestMatchers("/api/admin/catalog/**").authenticated()

                        // Dashboard requires authentication
                        .requestMatchers("/dashboard/**").authenticated()

                        // All API endpoints require authentication
                        .requestMatchers("/api/**").authenticated()

                        // Any other request requires authentication
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/keycloak")))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(userAuthoritiesMapper())))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/logout", "/api/logout", "/api/**"))
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
        scopeConverter.setAuthorityPrefix("SCOPE_");

        JwtAuthenticationConverter authConverter = new JwtAuthenticationConverter();
        authConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> auths = new HashSet<>();

            // 1. Extract scopes
            auths.addAll(scopeConverter.convert(jwt));

            // 2. Extract realm roles from realm_access.roles
            Object realmAccess = jwt.getClaims().get("realm_access");
            if (realmAccess instanceof Map<?, ?> realmMap) {
                Object realmRoles = realmMap.get("roles");
                if (realmRoles instanceof List<?>) {
                    ((List<?>) realmRoles).stream()
                            .map(Object::toString)
                            .map(r -> OmniConstants.ROLE_PREFIX_REALM + r)
                            .map(SimpleGrantedAuthority::new)
                            .forEach(auths::add);
                }
            }

            // 3. Extract client roles from resource_access.<clientId>.roles
            Object resourceAccess = jwt.getClaims().get("resource_access");
            if (resourceAccess instanceof Map<?, ?> raMap) {
                Object clientAccess = raMap.get(clientId);
                if (clientAccess instanceof Map<?, ?> clientMap) {
                    Object clientRoles = clientMap.get("roles");
                    if (clientRoles instanceof List<?>) {
                        ((List<?>) clientRoles).stream()
                                .map(Object::toString)
                                .map(r -> OmniConstants.ROLE_PREFIX_CLIENT + r)
                                .map(SimpleGrantedAuthority::new)
                                .forEach(auths::add);
                    }
                }
            }

            // Log extracted authorities
            log.info("Extracted authorities from JWT: {}", auths);

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
                    // Debug: Log all claims in ID token
                    log.info("ID Token claims: {}", oidcAuth.getIdToken().getClaims().keySet());

                    // Extract realm roles from realm_access.roles in ID token
                    Object realmAccess = oidcAuth.getIdToken().getClaims().get("realm_access");
                    if (realmAccess instanceof Map<?, ?> realmMap) {
                        Object realmRoles = realmMap.get("roles");
                        if (realmRoles instanceof List<?> rolesList) {
                            rolesList.forEach(role -> {
                                String roleName = role.toString();
                                mappedAuthorities.add(new SimpleGrantedAuthority(OmniConstants.ROLE_PREFIX_REALM + roleName));
                            });
                        }
                    }

                    // Extract client roles from resource_access.<clientId>.roles in ID token
                    Object resourceAccess = oidcAuth.getIdToken().getClaims().get("resource_access");
                    if (resourceAccess instanceof Map<?, ?> raMap) {
                        Object clientAccess = raMap.get(clientId);
                        if (clientAccess instanceof Map<?, ?> clientMap) {
                            Object clientRoles = clientMap.get("roles");
                            if (clientRoles instanceof List<?> clientRolesList) {
                                clientRolesList.forEach(role -> {
                                    String roleName = role.toString();
                                    mappedAuthorities.add(new SimpleGrantedAuthority(OmniConstants.ROLE_PREFIX_CLIENT + roleName));
                                });
                            }
                        }
                    }

                    // Log for debugging
                    log.info("OIDC Login - Mapped authorities: {}", mappedAuthorities);
                }
                mappedAuthorities.add(authority);
            });

            return mappedAuthorities;
        };
    }

}
