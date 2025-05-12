package com.omnip.beans;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.omnip.configs.AuditorAwareImpl;
import com.omnip.constants.OmniConstants;
import com.omnip.dtos.UserDTO;
import com.omnip.entities.Users;
import com.omnip.repositories.UsersRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")

public class Beans {

    private final JwtDecoder jwtDecoder;
    private final UsersRepository usersRepository;
    private final HttpServletRequest session;


    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private final UserDTO userDTO;

    public Beans(JwtDecoder jwtDecoder, UsersRepository usersRepository, HttpServletRequest session, UserDTO userDTO) {
        this.jwtDecoder = jwtDecoder;
        this.usersRepository = usersRepository;
        this.session = session;
        this.userDTO = userDTO;
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST,
            proxyMode = ScopedProxyMode.TARGET_CLASS)
    public UserDTO userDTO(HttpServletRequest request) {
        UserDTO dto = new UserDTO();

        // 1) Cek header Authorization
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            Jwt jwt = this.jwtDecoder.decode(token);
            String email = (String) jwt.getClaims().get("email");
            Users user = this.usersRepository.findByEmail(email);
            String providerUserId = jwt.getClaimAsString("sub");
            dto.setProviderUserId(providerUserId);
            dto.setUsername(user.getUsername());
            dto.setEmail(email);
            dto.setStore(user.getStore());
            dto.setFullname(user.getFullname());
            dto.setRoles(user.getRoles());
        } else if (this.session != null) {
            UserDTO userDTO = (UserDTO) this.session.getSession().getAttribute(OmniConstants.SESSION_USER_DTO);
            if (userDTO != null) {
                dto = userDTO;
            }

        }

        return dto;
    }

    @Bean
    public Keycloak keycloak() {
        // misal http://localhost:8888
        // misal "master"
        // "omnip-client" atau "omnip-admin-client"
        // secret yang kamu lihat di tab Credentials

        return KeycloakBuilder.builder()
                .serverUrl(this.serverUrl)       // misal http://localhost:8888
                .realm(this.realm)               // misal "master"
                .clientId(this.clientId)         // "omnip-client" atau "omnip-admin-client"
                .clientSecret(this.clientSecret)
                .username("sibebek")
                .grantType("password")
                .password("kozaninja")// secret yang kamu lihat di tab Credentials
                .build();
    }

    @Bean
    public CacheManager fastCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .maximumSize(100));
        return cacheManager;
    }

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl(this.userDTO);
    }

}
