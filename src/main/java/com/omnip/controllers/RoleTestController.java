package com.omnip.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
public class RoleTestController {

    /**
     * Endpoint ini HANYA bisa diakses jika user punya authority
     * "ROLE_delete_users".
     * Standard Spring Security check: hasRole('xxx') -> checks for ROLE_xxx.
     */
    @GetMapping("/api/test/delete-users")
    @PreAuthorize("hasRole('delete_users')")
    public ResponseEntity<String> testDeleteUsersPermission(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));

        return ResponseEntity.ok("ACCESS GRANTED! You have 'delete_users' role. Your authorities: " + authorities);
    }
}
