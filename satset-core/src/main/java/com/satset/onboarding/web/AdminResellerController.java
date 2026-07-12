package com.satset.onboarding.web;

import com.satset.onboarding.service.AdminOnboardingDomainService;
import com.satset.shared.constant.SatsetConstants;
import com.satset.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController

@Slf4j
public class AdminResellerController {

    private final AdminOnboardingDomainService adminOnboardingService;

    public AdminResellerController(AdminOnboardingDomainService adminOnboardingService) {
        this.adminOnboardingService = adminOnboardingService;
    }
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_CREATE_RESELLER + "')")
    @PostMapping("/api/admin/resellers")
    public ResponseEntity<Map<String, String>> createReseller(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("orgName") String orgName,
            @RequestParam("phone") String phone,
            @RequestParam(value = "upline", required = false) String upline) {

        try {
            adminOnboardingService.onboardReseller(username, email, orgName, phone, upline);
            return ResponseEntity
                    .ok(Map.of("status", "success", "message", "Reseller " + username + " berhasil dibuat."));
        } catch (BusinessException e) {
            log.warn("Business error creating reseller '{}': {}", username, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Gagal membuat reseller. Silakan coba lagi."));
        } catch (Exception e) {
            log.error("Unexpected error creating reseller '{}'", username, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", "Terjadi kesalahan sistem, silakan coba lagi."));
        }
    }
}
