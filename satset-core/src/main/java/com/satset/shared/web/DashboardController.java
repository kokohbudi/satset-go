package com.satset.shared.web;

import com.satset.onboarding.repository.StoreRepository;
import com.satset.shared.dto.UserDTO;
import com.satset.transaction.client.WalletGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Controller
@Slf4j
public class DashboardController {

    private static final Locale ID = Locale.forLanguageTag("id-ID");

    private final WalletGateway walletGateway;
    private final UserDTO userDTO;
    private final StoreRepository storeRepository;

    public DashboardController(WalletGateway walletGateway, UserDTO userDTO, StoreRepository storeRepository) {
        this.walletGateway = walletGateway;
        this.userDTO = userDTO;
        this.storeRepository = storeRepository;
    }

    @GetMapping("/")
    public String landingPage(Authentication authentication) {
        // Check if user is truly authenticated (not anonymous)
        if (isAuthenticated(authentication)) {
            log.debug("User {} is authenticated, redirecting to dashboard", authentication.getName());
            return "redirect:/dashboard";
        }
        return "landing";
    }

    /**
     * Helper method to check if user is properly authenticated
     * (not anonymous and actually authenticated)
     */
    private boolean isAuthenticated(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        return authentication.isAuthenticated();
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        log.info("Accessing dashboard");

        // Set page info for sidebar and header
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("breadcrumb", "Dashboard");
        model.addAttribute("totalBalance", formatBalance(userDTO.getWalletId()));
        model.addAttribute("totalResellers", NumberFormat.getInstance(ID).format(storeRepository.count()));

        return "pages/dashboard/index";
    }

    /** Fetch wallet balance as "Rp 50.000". Falls back to "Rp 0" on no wallet / fetch error — dashboard must not 500. */
    private String formatBalance(String walletId) {
        if (walletId == null) {
            return "Rp 0";
        }
        try {
            BigDecimal balance = walletGateway.getBalance(walletId);
            return "Rp " + NumberFormat.getInstance(ID).format(balance);
        } catch (Exception e) {
            log.error("Failed to fetch wallet balance for dashboard", e);
            return "Rp 0";
        }
    }
}
