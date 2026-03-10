package com.satset.wallet.adapter.in.web;

import com.satset.wallet.adapter.in.web.dto.BalanceResponse;
import com.satset.wallet.adapter.in.web.dto.MutationResponse;
import com.satset.wallet.adapter.in.web.dto.RefundRequest;
import com.satset.wallet.adapter.in.web.dto.WalletRequest;
import com.satset.wallet.domain.WalletMutationResult;
import com.satset.wallet.domain.model.MutationReferenceType;
import com.satset.wallet.domain.model.MutationType;
import com.satset.wallet.domain.model.WalletAccount;
import com.satset.wallet.domain.port.in.WalletUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/internal/wallet")
public class WalletInternalController {

    private final WalletUseCase walletUseCase;

    public WalletInternalController(WalletUseCase walletUseCase) {
        this.walletUseCase = walletUseCase;
    }

    @GetMapping("/balance/{walletId}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String walletId) {
        return ResponseEntity.ok(BalanceResponse.of(walletId, walletUseCase.getBalance(walletId)));
    }

    @PostMapping("/debit")
    public ResponseEntity<MutationResponse> debit(@Valid @RequestBody WalletRequest request) {
        MutationReferenceType refType = request.referenceType() != null
                ? request.referenceType() : MutationReferenceType.TRANSACTION;
        WalletMutationResult result = walletUseCase.debit(
                request.walletId(), request.amount(), request.referenceId(), refType, request.description());
        return ResponseEntity.ok(new MutationResponse(result.mutationId(), request.walletId(),
                request.amount(), MutationType.DEBIT, result.newBalance(),
                refType, request.referenceId(), request.description(), LocalDateTime.now()));
    }

    @PostMapping("/credit")
    public ResponseEntity<MutationResponse> credit(@Valid @RequestBody WalletRequest request) {
        MutationReferenceType refType = request.referenceType() != null
                ? request.referenceType() : MutationReferenceType.TOPUP;
        WalletMutationResult result = walletUseCase.credit(
                request.walletId(), request.amount(), request.referenceId(), refType, request.description());
        return ResponseEntity.ok(new MutationResponse(result.mutationId(), request.walletId(),
                request.amount(), MutationType.CREDIT, result.newBalance(),
                refType, request.referenceId(), request.description(), LocalDateTime.now()));
    }

    @PostMapping("/refund")
    public ResponseEntity<MutationResponse> refund(@Valid @RequestBody RefundRequest request) {
        WalletMutationResult result = walletUseCase.refund(
                request.walletId(), request.amount(), request.originalReferenceId(), request.description());
        return ResponseEntity.ok(new MutationResponse(result.mutationId(), request.walletId(),
                request.amount(), MutationType.REFUND, result.newBalance(),
                MutationReferenceType.REFUND, request.originalReferenceId(), request.description(), LocalDateTime.now()));
    }

    @GetMapping("/mutations/{walletId}")
    public ResponseEntity<List<MutationResponse>> getMutations(@PathVariable String walletId) {
        return ResponseEntity.ok(walletUseCase.getMutations(walletId).stream()
                .map(MutationResponse::from)
                .toList());
    }

    @PostMapping("/accounts")
    public ResponseEntity<WalletCreationResponse> createWallet() {
        WalletAccount account = walletUseCase.createWallet();
        return ResponseEntity.ok(new WalletCreationResponse(account.walletId()));
    }

    /**
     * Response DTO for wallet creation.
     */
    public record WalletCreationResponse(String walletId) {
    }
}
