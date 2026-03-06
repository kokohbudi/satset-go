package com.omnip.shared.bean;

import com.omnip.shared.config.AuditorAwareImpl;
import com.omnip.shared.constant.OmniConstants;
import com.omnip.shared.dto.UserDTO;
import com.omnip.identity.domain.model.Users;
import com.omnip.identity.domain.port.out.UserRepositoryPort;
import jakarta.servlet.http.HttpServletRequest;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.context.WebApplicationContext;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")

public class Beans {

    private final JwtDecoder jwtDecoder;
    private final UserRepositoryPort usersRepository;
    private final HttpServletRequest session;

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    public Beans(JwtDecoder jwtDecoder, UserRepositoryPort usersRepository, HttpServletRequest session) {
        this.jwtDecoder = jwtDecoder;
        this.usersRepository = usersRepository;
        this.session = session;
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
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
            dto.setStoreId(user.getStoreId());
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
                .serverUrl(this.serverUrl) // misal http://localhost:8888
                .realm(this.realm) // misal "master"
                .clientId(this.clientId) // harus client dengan service account enabled
                .clientSecret(this.clientSecret) // secret yang kamu lihat di tab Credentials
                .grantType("client_credentials") // menggunakan client credentials (tanpa username/password)
                .build();
    }

    // NOTE: fastCacheManager moved to PerformanceConfig.java for centralized cache
    // management

    @Bean
    public AuditorAware<String> auditorProvider(UserDTO userDTO) {
        return new AuditorAwareImpl(userDTO);
    }

}
