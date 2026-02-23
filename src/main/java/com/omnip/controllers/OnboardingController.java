package com.omnip.controllers;

import com.omnip.constants.OmniConstants;
import com.omnip.dtos.UserDTO;
import com.omnip.services.StoreOnboardingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Slf4j
public class OnboardingController {

    private final StoreOnboardingService storeOnboardingService;

    public OnboardingController(StoreOnboardingService storeOnboardingService) {
        this.storeOnboardingService = storeOnboardingService;
    }

    @GetMapping("/onboarding")
    public String showOnboardingForm(HttpSession session) {
        Boolean hasStore = (Boolean) session.getAttribute("hasStore");
        if (Boolean.TRUE.equals(hasStore)) {
            return "redirect:/dashboard";
        }
        return "pages/onboarding";
    }

    @PostMapping("/onboarding")
    public String processOnboarding(
            @RequestParam("orgName") String orgName,
            @RequestParam("phone") String phone,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        UserDTO userDTO = (UserDTO) session.getAttribute(OmniConstants.SESSION_USER_DTO);
        if (userDTO == null || userDTO.getProviderUserId() == null) {
            log.error("No user DTO or provider user ID found in session during onboarding");
            return "redirect:/login";
        }

        try {
            storeOnboardingService.onboardStore(userDTO.getProviderUserId(), orgName, phone);
            redirectAttributes.addFlashAttribute("toastMessage", "Toko \"" + orgName + "\" berhasil didaftarkan! 🎉");
            redirectAttributes.addFlashAttribute("toastType", "success");
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Error during store onboarding", e);
            redirectAttributes.addFlashAttribute("toastMessage", "Gagal mendaftarkan toko: " + e.getMessage());
            redirectAttributes.addFlashAttribute("toastType", "error");
            return "redirect:/onboarding";
        }
    }
}
