package com.satset.transaction.adapter.in.web;

import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.transaction.adapter.in.web.dto.PurchaseRequest;
import com.satset.transaction.adapter.in.web.dto.TopUpRequest;
import com.satset.transaction.adapter.in.web.dto.TransactionDTO;
import com.satset.transaction.adapter.out.wallet.WalletClientAdapter;
import com.satset.transaction.domain.model.TransactionSummary;
import com.satset.transaction.domain.service.TransactionDomainService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@PreAuthorize("isAuthenticated()")
public class TransactionController {

    private final TransactionDomainService transactionService;
    private final WalletClientAdapter balanceService;
    private final UserDTO userDTO;

    public TransactionController(TransactionDomainService transactionService,
            WalletClientAdapter balanceService,
            UserDTO userDTO) {
        this.transactionService = transactionService;
        this.balanceService = balanceService;
        this.userDTO = userDTO;
    }

    @PostMapping("/purchase")
    public ResponseEntity<Map<String, Object>> purchase(@Valid @RequestBody PurchaseRequest request)
            throws InsufficientBalanceException {

        TransactionSummary summary = transactionService.createPurchase(
                getStoreId(), getWalletId(), request.denomId(), request.targetNumber());

        Map<String, Object> response = new HashMap<>();
        response.put("status", summary.status().name());
        response.put("transactionId", summary.id());
        response.put("total", summary.total());
        response.put("providerRef", summary.providerRef());
        response.put("serialNumber", summary.serialNumber());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/topup")
    public ResponseEntity<Map<String, Object>> topUp(@Valid @RequestBody TopUpRequest request) {

        String walletId = getWalletId();
        transactionService.topUp(walletId, request.amount(), request.description());

        BigDecimal balance = balanceService.getBalance(walletId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Top-up berhasil");
        response.put("balance", balance);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance() {

        String walletId = getWalletId();
        BigDecimal balance = balanceService.getBalance(walletId);

        Map<String, Object> response = new HashMap<>();
        response.put("walletId", walletId);
        response.put("balance", balance);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable UUID id) {
        TransactionSummary summary = transactionService.getTransaction(id, getStoreId());
        return ResponseEntity.ok(toDTO(summary));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TransactionDTO>> getTransactionHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<TransactionDTO> page = transactionService.getTransactionHistory(getStoreId(), pageable)
                .map(this::toDTO);
        return ResponseEntity.ok(page);
    }

    // ==================== Mappers ====================

    private TransactionDTO toDTO(TransactionSummary s) {
        return new TransactionDTO(
                s.id(),
                s.storeId(),
                s.targetNumber(),
                s.denomName(),
                s.productName(),
                s.price(),
                s.adminFee(),
                s.total(),
                s.status(),
                s.providerRef(),
                s.serialNumber(),
                s.createdAt());
    }

    private UUID getStoreId() {
        if (userDTO.getStoreId() == null) {
            throw new ResourceNotFoundException("Store", "current user has no store");
        }
        return userDTO.getStoreId();
    }

    private String getWalletId() {
        if (userDTO.getWalletId() == null) {
            throw new ResourceNotFoundException("Wallet", "current user has no wallet");
        }
        return userDTO.getWalletId();
    }
}
