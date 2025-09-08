package com.omnip.configs;

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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableCaching
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(JwtAuthenticationConverter jwtAuthenticationConverter, HttpSecurity http, LogoutSuccessHandler logoutSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/dashboard").authenticated()
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
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET")));
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

        // 2) converter untuk realm roles ("realm_access.roles" -> ROLE_…)
        JwtGrantedAuthoritiesConverter realmRoleConverter = new JwtGrantedAuthoritiesConverter();
        realmRoleConverter.setAuthoritiesClaimName("realm_access.roles");
        realmRoleConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authConverter = new JwtAuthenticationConverter();
        authConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> auths = new HashSet<>();
            // scopes
            auths.addAll(scopeConverter.convert(jwt));
            // realm roles
            auths.addAll(realmRoleConverter.convert(jwt));
            // client-level roles (resource_access)
            Object ra = jwt.getClaims().get("resource_access");
            if (ra instanceof Map<?, ?> resAccess) {
                Object cli = resAccess.get("omnip-client");
                if (cli instanceof Map<?, ?>) {
                    Object roles = ((Map<?, ?>) cli).get("roles");
                    if (roles instanceof List<?>) {
                        ((List<?>) roles).stream()
                                .map(Object::toString)
                                .map(r -> "ROLE_" + r)
                                .map(SimpleGrantedAuthority::new)
                                .forEach(auths::add);
                    }
                }
            }
            return auths;
        });
        return authConverter;
    }


}
