package com.omnip.onboarding.adapter.in.web;

import com.omnip.shared.exception.BusinessException;
import com.omnip.onboarding.domain.port.in.AdminOnboardingUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/resellers")
@Slf4j
public class AdminResellerController {

    private final AdminOnboardingUseCase adminOnboardingUseCase;

    public AdminResellerController(AdminOnboardingUseCase adminOnboardingUseCase) {
        this.adminOnboardingUseCase = adminOnboardingUseCase;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createReseller(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("orgName") String orgName,
            @RequestParam("phone") String phone,
            @RequestParam(value = "upline", required = false) String upline) {

        try {
            adminOnboardingUseCase.onboardReseller(username, email, orgName, phone, upline);
            return ResponseEntity
                    .ok(Map.of("status", "success", "message", "Reseller " + username + " berhasil dibuat."));
        } catch (BusinessException e) {
            log.warn("Business error creating reseller '{}': {}", username, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating reseller '{}'", username, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", "Terjadi kesalahan sistem, silakan coba lagi."));
        }
    }
}
