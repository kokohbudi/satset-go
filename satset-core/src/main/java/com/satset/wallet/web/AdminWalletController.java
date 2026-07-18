package com.satset.wallet.web;

import com.satset.shared.constant.SatsetConstants;
import com.satset.wallet.dto.AdjustBalanceRequest;
import com.satset.wallet.dto.WalletMutationDTO;
import com.satset.wallet.model.MutationReferenceType;
import com.satset.wallet.service.account.WalletMutationResult;
import com.satset.wallet.service.account.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-only manual balance injection (top-up without payment).
 * Recorded as a CREDIT mutation with reference type ADJUSTMENT for audit.
 */
@RestController
@RequestMapping("/api/admin/wallets")
@PreAuthorize("hasRole('" + SatsetConstants.PERM_ADJUST_BALANCE + "')")
public class AdminWalletController {

    private final WalletService walletService;

    public AdminWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/{walletId}/adjust")
    public ResponseEntity<Map<String, Object>> adjust(@PathVariable String walletId,
            @Valid @RequestBody AdjustBalanceRequest request) {

        // Random reference per call: each manual adjustment is a distinct, non-idempotent event.
        WalletMutationResult result = walletService.credit(walletId, request.amount(), UUID.randomUUID(),
                MutationReferenceType.ADJUSTMENT, request.description());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "walletId", walletId,
                "balance", result.newBalance(),
                "mutationId", result.mutationId()));
    }

    @GetMapping("/{walletId}/mutations")
    public ResponseEntity<List<WalletMutationDTO>> mutations(@PathVariable String walletId) {
        return ResponseEntity.ok(walletService.getMutations(walletId).stream()
                .map(WalletMutationDTO::from)
                .toList());
    }
}
