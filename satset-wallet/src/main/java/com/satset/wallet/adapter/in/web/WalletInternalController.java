package com.satset.wallet.adapter.in.web;

import com.satset.wallet.adapter.in.web.dto.BalanceResponse;
import com.satset.wallet.adapter.in.web.dto.MutationResponse;
import com.satset.wallet.adapter.in.web.dto.RefundRequest;
import com.satset.wallet.adapter.in.web.dto.WalletRequest;
import com.satset.wallet.domain.WalletMutationResult;
import com.satset.wallet.domain.model.MutationReferenceType;
import com.satset.wallet.domain.model.MutationType;
import com.satset.wallet.domain.port.in.WalletUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/wallet")
public class WalletInternalController {

    private final WalletUseCase walletUseCase;

    public WalletInternalController(WalletUseCase walletUseCase) {
        this.walletUseCase = walletUseCase;
    }

    @GetMapping("/balance/{storeId}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID storeId) {
        return ResponseEntity.ok(BalanceResponse.of(storeId, walletUseCase.getBalance(storeId)));
    }

    @PostMapping("/debit")
    public ResponseEntity<MutationResponse> debit(@Valid @RequestBody WalletRequest request) {
        MutationReferenceType refType = request.referenceType() != null
                ? request.referenceType() : MutationReferenceType.TRANSACTION;
        WalletMutationResult result = walletUseCase.debit(
                request.storeId(), request.amount(), request.referenceId(), refType, request.description());
        return ResponseEntity.ok(new MutationResponse(result.mutationId(), request.storeId(),
                request.amount(), MutationType.DEBIT, result.newBalance(),
                refType, request.referenceId(), request.description(), LocalDateTime.now()));
    }

    @PostMapping("/credit")
    public ResponseEntity<MutationResponse> credit(@Valid @RequestBody WalletRequest request) {
        MutationReferenceType refType = request.referenceType() != null
                ? request.referenceType() : MutationReferenceType.TOPUP;
        WalletMutationResult result = walletUseCase.credit(
                request.storeId(), request.amount(), request.referenceId(), refType, request.description());
        return ResponseEntity.ok(new MutationResponse(result.mutationId(), request.storeId(),
                request.amount(), MutationType.CREDIT, result.newBalance(),
                refType, request.referenceId(), request.description(), LocalDateTime.now()));
    }

    @PostMapping("/refund")
    public ResponseEntity<MutationResponse> refund(@Valid @RequestBody RefundRequest request) {
        WalletMutationResult result = walletUseCase.refund(
                request.storeId(), request.amount(), request.originalReferenceId(), request.description());
        return ResponseEntity.ok(new MutationResponse(result.mutationId(), request.storeId(),
                request.amount(), MutationType.REFUND, result.newBalance(),
                MutationReferenceType.REFUND, request.originalReferenceId(), request.description(), LocalDateTime.now()));
    }

    @GetMapping("/mutations/{storeId}")
    public ResponseEntity<List<MutationResponse>> getMutations(@PathVariable UUID storeId) {
        return ResponseEntity.ok(walletUseCase.getMutations(storeId).stream()
                .map(MutationResponse::from)
                .toList());
    }
}