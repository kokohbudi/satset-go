package com.omnip.controllers;

import com.omnip.dtos.UserDTO;
import com.omnip.exceptions.BusinessException;
import com.omnip.services.UserManagementService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller untuk menangani operasi-operasi terkait pengguna.
 * Menyediakan endpoint untuk manajemen akun, otentikasi, dan debugging.
 */
@Slf4j
@RestController
public class UserController {
    private final JwtDecoder jwtDecoder;
    private final UserDTO userDTO;
    private final UserManagementService userManagementService;

    /**
     * Konstruktor dengan dependency injection.
     *
     * @param jwtDecoder            Decoder untuk memproses token JWT
     * @param userDTO               DTO yang berisi informasi pengguna saat ini
     * @param userManagementService Service untuk manajemen pengguna
     */
    public UserController(JwtDecoder jwtDecoder, UserDTO userDTO, UserManagementService userManagementService) {
        this.jwtDecoder = jwtDecoder;
        this.userDTO = userDTO;
        this.userManagementService = userManagementService;
    }

    /**
     * Mendapatkan informasi akun pengguna yang sedang login.
     *
     * @return UserDTO berisi informasi pengguna saat ini
     */
    @GetMapping("/api/users")
    public UserDTO account() {
        return this.userDTO;
    }

    /**
     * Membuat akun pengguna baru.
     *
     * @param reqUserDTO DTO yang berisi informasi pengguna yang akan dibuat
     * @return UserDTO berisi informasi pengguna yang telah dibuat dengan status operasi
     */
    @PostMapping("/api/users")
    public UserDTO createAccount(@RequestBody UserDTO reqUserDTO) {
        return this.userManagementService.createNewUser(reqUserDTO);
    }

    /**
     * Mengubah password pengguna.
     *
     * @param reqUserDTO DTO yang berisi email dan password baru
     * @return UserDTO berisi status operasi perubahan password
     */
    @PutMapping("/api/users/password")
    public UserDTO changePassword(@RequestBody UserDTO reqUserDTO) {
        return this.userManagementService.changePassword(reqUserDTO);
    }

    /**
     * Mengubah status aktif pengguna.
     * Hanya dapat diakses oleh pengguna dengan role 'omnip-store-admin'.
     *
     * @param email  Email pengguna yang statusnya akan diubah
     * @param status Status baru (true untuk aktif, false untuk tidak aktif)
     * @return UserDTO berisi status operasi pengubahan status
     */
    @PutMapping("/api/users/{email}/status/{status}")
    @PreAuthorize("hasRole('ROLE_omnip-store-admin')")
    public UserDTO setUserStatus(@PathVariable String email, @PathVariable boolean status) {
        UserDTO requestedUserDTO = new UserDTO();
        requestedUserDTO.setEmail(email);
        requestedUserDTO.setActive(status);
        try {
            this.userManagementService.setUserStatus(requestedUserDTO);
            requestedUserDTO.setStatus("success");
            requestedUserDTO.setMessage("Status pengguna berhasil diubah");
            return requestedUserDTO;
        } catch (BusinessException e) {
            requestedUserDTO.setStatus("failed");
            requestedUserDTO.setMessage(e.getErrorMessage());
            return requestedUserDTO;
        }
    }

    /**
     * Endpoint debugging untuk melihat role pengguna dari token.
     *
     * @param client OAuth2AuthorizedClient untuk mendapatkan token
     * @return Map berisi informasi token dan role
     */
    @GetMapping("/api/users/roles")
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

    /**
     * Endpoint untuk mendapatkan informasi pengguna dari JWT atau session.
     *
     * @param jwt     JWT dari request saat ini (jika ada)
     * @param session HttpSession untuk akses session data
     * @return String berisi informasi pengguna atau pesan error
     */
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

    /**
     * Endpoint pengujian untuk verifikasi akses role.
     * Hanya dapat diakses oleh pengguna dengan role 'omnip-store-admin'.
     *
     * @return ResponseEntity berisi status operasi
     */
    @GetMapping("/api/test")
    @PreAuthorize("hasRole('ROLE_omnip-store-admin')")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("done");
    }

    /**
     * Endpoint debugging untuk melihat seluruh klaim JWT.
     *
     * @param jwt JWT dari request saat ini
     * @return Map berisi semua klaim JWT
     */
    @GetMapping("/api/debug-jwt")
    public Map<String, Object> debugJwt(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getClaims();
    }
}