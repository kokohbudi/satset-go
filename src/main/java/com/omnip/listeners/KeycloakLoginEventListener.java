package com.omnip.listeners;

import com.omnip.constant.OmniConstants;
import com.omnip.dto.UserDTO;
import com.omnip.entities.Users;
import com.omnip.services.RegistrationService;
import com.omnip.services.StoreService;
import com.omnip.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

@Component
public class KeycloakLoginEventListener {
    private final JwtDecoder jwtDecoder;
    private static final Logger logger = LoggerFactory.getLogger(KeycloakLoginEventListener.class);
    //    private final UsersRepository usersRepository;
    private final UserService userService;
    private final StoreService storeService;
    private final RegistrationService registrationService;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String keycloakClientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;


    public KeycloakLoginEventListener(JwtDecoder jwtDecoder, UserService userService, StoreService storeService, RegistrationService registrationService) {
        this.jwtDecoder = jwtDecoder;
//        this.usersRepository = usersRepository;
        this.userService = userService;
        this.storeService = storeService;
        this.registrationService = registrationService;
    }

    @EventListener
    @Transactional
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication() instanceof OAuth2LoginAuthenticationToken authentication) {

            // Get the OAuth2User which contains user details
            OAuth2User oauth2User = authentication.getPrincipal();

            // Extract user information from the OAuth2User
            String email = this.extractEmail(oauth2User);
            String username = this.extractUsername(oauth2User);
            String fullName = this.extractFullName(oauth2User);
            Jwt jwt = this.jwtDecoder.decode(authentication.getAccessToken().getTokenValue());
            String providerUserId = this.extractProviderUserId(jwt);
            Users user;
            if (email != null) {
                logger.info("Processing Keycloak login for user: {}", email);
                boolean isEmailRegistered = this.registrationService.isEmailRegistered(email);
                List roles;
                if (isEmailRegistered) {
                    user = this.userService.findByEmail(email);
                    email = user.getEmail();
                    username = user.getUsername();
                    fullName = user.getFullname();
                    roles = user.getRoles();
                } else {
                    roles = this.extractRolesFromJwt(jwt);
                    Map returnMap = this.registrationService.registerNewStore(email, fullName, roles, OmniConstants.REGISTRATION_CHANNEL_KEYCLOAK, providerUserId);
                    user = (Users) returnMap.get("user");
                }
                ServletRequestAttributes attrs = (ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    HttpSession session = request.getSession();
                    UserDTO userDTO = new UserDTO();

                    userDTO.setEmail(email);
                    userDTO.setUsername(username);
                    userDTO.setFullname(fullName);
                    userDTO.setRoles(roles);
                    userDTO.setStore(user.getStore());
                    userDTO.setProviderUserId(providerUserId);
                    session.setAttribute(OmniConstants.SESSION_USER_DTO, userDTO);
                }
            } else {
                logger.warn("Couldn't extract email from login event");
            }
        }
    }

    public List<String> extractRolesFromJwt(Jwt jwt) {
        try {
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess == null) return List.of();

            @SuppressWarnings("unchecked")
            Map<String, Object> clientResource = (Map<String, Object>) resourceAccess.get(this.clientId);
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

    public String extractProviderUserId(Jwt jwt) {
        return jwt.getClaimAsString("sub");
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
        String email = this.extractEmail(oauth2User);
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
        return this.extractUsername(oauth2User);
    }


}