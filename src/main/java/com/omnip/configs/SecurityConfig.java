package com.omnip.configs;

import com.omnip.services.RoleIntegrationService;
import lombok.extern.slf4j.Slf4j;
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

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableCaching
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig {
    
    private final RoleIntegrationService roleIntegrationService;

    public SecurityConfig(RoleIntegrationService roleIntegrationService) {
        this.roleIntegrationService = roleIntegrationService;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(JwtAuthenticationConverter jwtAuthenticationConverter, HttpSecurity http, LogoutSuccessHandler logoutSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/", "/login", "/error", "/webjars/**", "/css/**", "/js/**", "/images/**").permitAll()
                        
                        // Role management endpoints - temporarily allow all for development
                        .requestMatchers("/admin/roles/**").permitAll()
                        .requestMatchers("/api/roles/**").permitAll()
                        
                        // User management endpoints - temporarily allow all for development
                        .requestMatchers("/api/users/**").permitAll()
                        
                        // Dashboard and general API endpoints - temporarily allow all for development
                        .requestMatchers("/dashboard/**").authenticated()
                        .requestMatchers("/api/**").permitAll()
                        
                        // Any other request
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/keycloak")
                        )
                )
                .oauth2Login(withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        ))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessHandler(logoutSuccessHandler));
        return http.build();
    }

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http,
//                                                   JwtAuthenticationConverter jwtAuthenticationConverter,
//                                                   LogoutSuccessHandler logoutSuccessHandler) throws Exception {
//        http
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/**").authenticated()
//                        .anyRequest().permitAll()
//                )
//                .oauth2ResourceServer(oauth2 -> oauth2
//                        .jwt(jwt -> jwt
//                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
//                        )
//                )
//        // ... oauth2Login, exceptionHandling, logout, dll.
//        ;
//        return http.build();
//    }

    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clients) {
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(clients);
        // Setelah logout di Keycloak, redirect kembali ke home (atau halaman login)
        handler.setPostLogoutRedirectUri("http://localhost:8080/logout");
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
                            .map(r -> "ROLE_" + r)
                            .map(SimpleGrantedAuthority::new)
                            .forEach(auths::add);
                }
            }
            
            // 3. Extract client roles from resource_access.omnip-client.roles
            Object resourceAccess = jwt.getClaims().get("resource_access");
            if (resourceAccess instanceof Map<?, ?> resAccess) {
                Object clientAccess = resAccess.get("omnip-client");
                if (clientAccess instanceof Map<?, ?> clientMap) {
                    Object clientRoles = clientMap.get("roles");
                    if (clientRoles instanceof List<?>) {
                        ((List<?>) clientRoles).stream()
                                .map(Object::toString)
                                .map(r -> "ROLE_" + r)
                                .map(SimpleGrantedAuthority::new)
                                .forEach(auths::add);
                    }
                }
            }
            
            // Debug: Log JWT claims and extracted authorities
            System.out.println("=== JWT CLAIMS DEBUG ===");
            System.out.println("All claims: " + jwt.getClaims().keySet());
            System.out.println("realm_access: " + jwt.getClaims().get("realm_access"));
            System.out.println("resource_access: " + jwt.getClaims().get("resource_access"));
            System.out.println("Extracted authorities: " + auths);
            System.out.println("========================");
            
            return auths;
        });
        return authConverter;
    }


}
