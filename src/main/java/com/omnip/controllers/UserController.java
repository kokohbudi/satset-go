package com.omnip.controllers;

import com.omnip.BusinessException;
import com.omnip.business.UserManagementBusiness;
import com.omnip.dto.UserDTO;
import com.omnip.entities.Users;
import com.omnip.services.KeycloakAdminClientService;
import com.omnip.services.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class UserController {
    private final JwtDecoder jwtDecoder;
    private final UserDTO userDTO;
    private final KeycloakAdminClientService keycloakAdminClientService;
    private final UserManagementBusiness userManagementBusiness;
    private final UserService userService;

    @Value("#{'${omnip.allowed-role.change-password}'.split(',')}")
    private List<String> allowedChangePasswordRoles;

    public UserController(JwtDecoder jwtDecoder, UserDTO userDTO, KeycloakAdminClientService keycloakAdminClientService, UserManagementBusiness userManagementBusiness, UserService userService) {
        this.jwtDecoder = jwtDecoder;
        this.userDTO = userDTO;
        this.keycloakAdminClientService = keycloakAdminClientService;
        this.userManagementBusiness = userManagementBusiness;
        this.userService = userService;
    }

    @GetMapping("/api/account")
    public UserDTO account() {
        return this.userDTO;
    }

    @PostMapping("/api/account")
    public UserDTO createAccount(@RequestBody UserDTO reqUserDTO) {
        UserDTO createdUserDTO = null;
        try {
            createdUserDTO = this.keycloakAdminClientService.createUser(reqUserDTO.getUsername(), reqUserDTO.getFullname(), reqUserDTO.getEmail(), reqUserDTO.getPassword(), reqUserDTO.getRoles().get(0));
        } catch (BusinessException e) {
            UserDTO userDTOReturn = new UserDTO();
            userDTOReturn.setStatus("failed");
            userDTOReturn.setMessage(e.getErrorMessage());
            return userDTOReturn;
        }
        Users user = new Users();
        user.setEmail(reqUserDTO.getEmail());
        user.setUsername(reqUserDTO.getUsername());
        user.setFullname(reqUserDTO.getFullname());
        user.setRoles(reqUserDTO.getRoles());
        user.setStore(this.userDTO.getStore());
        user.setProviderUserId(createdUserDTO.getProviderUserId());
        user.setRegistrationChannel("omnia");

        this.userDTO.setPassword(null);

        this.userService.createNewUser(user);
        return createdUserDTO;
    }


    @PutMapping("/api/password")
    public UserDTO changePassword(@RequestBody UserDTO reqUserDTO) {

        String providerUserId;
        UserDTO userDTOReturn = new UserDTO();
        try {
            providerUserId = this.userService.getProviderUseIdChangePassword(this.userDTO, reqUserDTO, this.allowedChangePasswordRoles);
            Users user = this.keycloakAdminClientService.changeUserPassword(providerUserId, reqUserDTO.getPassword());
            reqUserDTO.setStatus("success");
            reqUserDTO.setMessage("Password berhasil diubah");
            reqUserDTO.setProviderUserId(user.getProviderUserId());
            reqUserDTO.setUsername(user.getUsername());
            reqUserDTO.setFullname(user.getFullname());
            reqUserDTO.setRoles(user.getRoles());
            reqUserDTO.setEmail(user.getEmail());
            reqUserDTO.setStore(user.getStore());
            reqUserDTO.setPassword(null);
            reqUserDTO.setEmail(this.userDTO.getEmail());
            return reqUserDTO;
        } catch (BusinessException e) {
            userDTOReturn.setStatus("failed");
            userDTOReturn.setMessage(e.getErrorMessage());
        } catch (Exception e) {
            userDTOReturn.setStatus("failed");
            log.error(e.getMessage(), e);
        }
        return userDTOReturn;
    }


    @GetMapping("/api/roles")
    public Map debugRoles(@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String token = client.getAccessToken().getTokenValue();
        Jwt jwt = this.jwtDecoder.decode(token);

        // Gabungkan header + claims ke dalam satu map
        Map<String, Object> result = new HashMap<>();
        result.put("tokenValue", token);
        result.put("headers", jwt.getHeaders());
        result.put("claims", jwt.getClaims().get("resource_access"));
        return result;
    }

    @GetMapping("/api/jos")
    public String getUserInfo(@AuthenticationPrincipal Jwt jwt, HttpSession session) {
        // Cek apakah JWT ada di header atau session
        if (jwt != null) {
            return "Hello " + jwt.getClaim("name") + " (from JWT)";
        } else {
            String token = (String) session.getAttribute("access_token");
            return token != null ? "Hello (from session)" : "No token found";
        }
    }
}
