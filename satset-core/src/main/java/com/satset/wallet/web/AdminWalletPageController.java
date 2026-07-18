package com.satset.wallet.web;

import com.satset.onboarding.model.Stores;
import com.satset.onboarding.repository.StoreRepository;
import com.satset.shared.constant.SatsetConstants;
import com.satset.wallet.dto.WalletAccountDTO;
import com.satset.wallet.model.WalletAccountEntity;
import com.satset.wallet.service.account.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class AdminWalletPageController {

    private final WalletService walletService;
    private final StoreRepository storeRepository;

    public AdminWalletPageController(WalletService walletService, StoreRepository storeRepository) {
        this.walletService = walletService;
        this.storeRepository = storeRepository;
    }

    @GetMapping("/admin/wallets/adjust")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_ADJUST_BALANCE + "')")
    public String adjustPage(Model model) {
        log.info("Accessing admin wallet inject page");

        List<WalletAccountEntity> accounts = walletService.listAccounts();

        // Correlate wallet -> store (email/name) via core DB. Wallet & store live in separate DBs.
        Map<String, Stores> byWallet = storeRepository.findByWalletIdIn(
                        accounts.stream().map(WalletAccountEntity::getWalletId).toList())
                .stream()
                .collect(Collectors.toMap(Stores::getWalletId, Function.identity(), (a, b) -> a));

        List<WalletAccountDTO> wallets = accounts.stream()
                .map(a -> {
                    Stores s = byWallet.get(a.getWalletId());
                    return new WalletAccountDTO(a.getWalletId(), a.getBalance(), a.getUpdatedAt(),
                            s != null ? s.getName() : null, s != null ? s.getEmail() : null);
                })
                .toList();

        model.addAttribute("currentPage", "wallets");
        model.addAttribute("breadcrumb", "Inject Saldo");
        model.addAttribute("initialWallets", wallets);
        return "pages/admin/wallet-adjust";
    }
}
