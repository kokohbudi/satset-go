package com.satset.onboarding.adapter.in.web;

import com.satset.onboarding.domain.port.in.SelfOnboardingUseCase;
import com.satset.shared.constant.OmniConstants;
import com.satset.shared.dto.UserDTO;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Slf4j
public class OnboardingController {

    private final SelfOnboardingUseCase selfOnboardingUseCase;

    public OnboardingController(SelfOnboardingUseCase selfOnboardingUseCase) {
        this.selfOnboardingUseCase = selfOnboardingUseCase;
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
            selfOnboardingUseCase.onboardStore(userDTO.getProviderUserId(), orgName, phone);
            redirectAttributes.addFlashAttribute("toastMessage", "Toko \"" + orgName + "\" berhasil didaftarkan! 🎉");
            redirectAttributes.addFlashAttribute("toastType", "success");
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Error during store onboarding", e);
            redirectAttributes.addFlashAttribute("toastMessage", "Gagal mendaftarkan toko. Silakan coba lagi.");
            redirectAttributes.addFlashAttribute("toastType", "error");
            return "redirect:/onboarding";
        }
    }
}
