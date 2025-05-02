package com.omnip.listeners;

import com.omnip.entities.Users;
import com.omnip.repositories.UsersRepository;
import com.omnip.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KeycloakLoginEventListener {
    private final JwtDecoder jwtDecoder;
    private static final Logger logger = LoggerFactory.getLogger(KeycloakLoginEventListener.class);
    private final UsersRepository usersRepository;
    private final UserService userService;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String keycloakClientId;

    public KeycloakLoginEventListener(JwtDecoder jwtDecoder, UsersRepository usersRepository, UserService userService) {
        this.jwtDecoder = jwtDecoder;
        this.usersRepository = usersRepository;
        this.userService = userService;
    }

    @EventListener
    @Transactional
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        // Check if this is an OAuth2 login event
        if (event.getAuthentication() instanceof OAuth2LoginAuthenticationToken) {
            OAuth2LoginAuthenticationToken authentication = (OAuth2LoginAuthenticationToken) event.getAuthentication();

            // Get the OAuth2User which contains user details
            OAuth2User oauth2User = authentication.getPrincipal();

            // Extract user information from the OAuth2User
            String email = extractEmail(oauth2User);
            String username = extractUsername(oauth2User);
            String fullName = extractFullName(oauth2User);

            if (email != null) {
                logger.info("Processing Keycloak login for user: {}", email);

                // Check if user already exists in our database
                Optional<Users> existingUser = Optional.ofNullable(usersRepository.findByEmail(email));

                if (existingUser.isPresent()) {
                    // Update existing user
                    userService.updateExistingUser(existingUser.get(), username, fullName);
                } else {
                    // Create new user
                    List<String> roles = extractRolesFromJwt(jwtDecoder.decode(authentication.getAccessToken().getTokenValue()));
                    userService.createNewUser(email, username, fullName, roles);
                }
            } else {
                logger.warn("Couldn't extract email from login event");
            }
        }
    }

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    public List<String> extractRolesFromJwt(Jwt jwt) {
        try {
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess == null) return List.of();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> clientResource = (Map<String, Object>) resourceAccess.get(clientId);
            if (clientResource == null) return List.of();
            
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) clientResource.get("roles");
            return roles != null ? roles : List.of();
        } catch (Exception e) {
            // Log the exception
            logger.warn("Failed to extract roles from JWT: {}", e.getMessage());
            return List.of();
        }
    }



    private String extractEmail(OAuth2User oauth2User) {
        // Try to get email from different possible attributes
        if (oauth2User instanceof OidcUser) {
            // OIDC standard claims
            return ((OidcUser) oauth2User).getEmail();
        }

        // Try common attribute names
        Map<String, Object> attributes = oauth2User.getAttributes();
        if (attributes.containsKey("email")) {
            return (String) attributes.get("email");
        } else if (attributes.containsKey("mail")) {
            return (String) attributes.get("mail");
        } else if (attributes.containsKey("emailAddress")) {
            return (String) attributes.get("emailAddress");
        }

        return null;
    }

    private String extractUsername(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();

        // Try common username attributes
        if (attributes.containsKey("preferred_username")) {
            return (String) attributes.get("preferred_username");
        } else if (attributes.containsKey("username")) {
            return (String) attributes.get("username");
        } else if (attributes.containsKey("login")) {
            return (String) attributes.get("login");
        }

        // If no username found, use email prefix
        String email = extractEmail(oauth2User);
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }

        return "user" + System.currentTimeMillis(); // Fallback
    }

    private String extractFullName(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();

        // Try to get full name from different attributes
        if (attributes.containsKey("name")) {
            return (String) attributes.get("name");
        } else if (attributes.containsKey("fullname")) {
            return (String) attributes.get("fullname");
        }

        // Try to combine given_name and family_name
        String firstName = (String) attributes.getOrDefault("given_name", "");
        String lastName = (String) attributes.getOrDefault("family_name", "");

        if (!firstName.isEmpty() || !lastName.isEmpty()) {
            return (firstName + " " + lastName).trim();
        }

        // Use username as fallback
        return extractUsername(oauth2User);
    }


}