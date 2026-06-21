package com.satset.wallet.web;

import com.satset.wallet.web.dto.BalanceResponse;
import com.satset.wallet.web.dto.MutationResponse;
import com.satset.wallet.web.dto.RefundRequest;
import com.satset.wallet.web.dto.WalletRequest;
import com.satset.wallet.service.WalletMutationResult;
import com.satset.wallet.service.WalletService;
import com.satset.wallet.model.MutationReferenceType;
import com.satset.wallet.model.MutationType;
import com.satset.wallet.model.WalletAccountEntity;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/internal/wallet")
public class WalletInternalController {

    private final WalletService walletService;

    public WalletInternalController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance/{walletId}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String walletId) {
        return ResponseEntity.ok(BalanceResponse.of(walletId, walletService.getBalance(walletId)));
    }

    @PostMapping("/debit")
    public ResponseEntity<MutationResponse> debit(@Valid @RequestBody WalletRequest request) {
        MutationReferenceType refType = request.referenceType() != null
                ? request.referenceType() : MutationReferenceType.TRANSACTION;
        WalletMutationResult result = walletService.debit(
                request.walletId(), request.amount(), request.referenceId(), refType, request.description());
        return ResponseEntity.ok(new MutationResponse(result.mutationId(), request.walletId(),
                request.amount(), MutationType.DEBIT, result.newBalance(),
                refType, request.referenceId(), request.description(), LocalDateTime.now()));
    }

    @PostMapping("/credit")
    public ResponseEntity<MutationResponse> credit(@Valid @RequestBody WalletRequest request) {
        MutationReferenceType refType = request.referenceType() != null
                ? request.referenceType() : MutationReferenceType.TOPUP;
        WalletMutationResult result = walletService.credit(
                request.walletId(), request.amount(), request.referenceId(), refType, request.description());
        return ResponseEntity.ok(new MutationResponse(result.mutationId(), request.walletId(),
                request.amount(), MutationType.CREDIT, result.newBalance(),
                refType, request.referenceId(), request.description(), LocalDateTime.now()));
    }

    @PostMapping("/refund")
    public ResponseEntity<MutationResponse> refund(@Valid @RequestBody RefundRequest request) {
        WalletMutationResult result = walletService.refund(
                request.walletId(), request.amount(), request.originalReferenceId(), request.description());
        return ResponseEntity.ok(new MutationResponse(result.mutationId(), request.walletId(),
                request.amount(), MutationType.REFUND, result.newBalance(),
                MutationReferenceType.REFUND, request.originalReferenceId(), request.description(), LocalDateTime.now()));
    }

    @GetMapping("/mutations/{walletId}")
    public ResponseEntity<List<MutationResponse>> getMutations(@PathVariable String walletId) {
        return ResponseEntity.ok(walletService.getMutations(walletId).stream()
                .map(MutationResponse::from)
                .toList());
    }

    @PostMapping("/accounts")
    public ResponseEntity<WalletCreationResponse> createWallet() {
        WalletAccountEntity account = walletService.createWallet();
        return ResponseEntity.ok(new WalletCreationResponse(account.getWalletId()));
    }

    /**
     * Response DTO for wallet creation.
     */
    public record WalletCreationResponse(String walletId) {
    }
}
