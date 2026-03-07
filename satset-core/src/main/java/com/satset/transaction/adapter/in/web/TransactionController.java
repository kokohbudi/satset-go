package com.satset.transaction.adapter.in.web;

import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.transaction.adapter.in.web.dto.PurchaseRequest;
import com.satset.transaction.adapter.in.web.dto.TopUpRequest;
import com.satset.transaction.adapter.in.web.dto.TransactionDTO;
import com.satset.transaction.domain.model.TransactionSummary;
import com.satset.transaction.domain.port.in.BalanceManagementUseCase;
import com.satset.transaction.domain.port.in.PurchaseUseCase;
import com.satset.transaction.domain.port.in.TopUpUseCase;
import com.satset.transaction.domain.port.in.TransactionQueryUseCase;
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

    private final PurchaseUseCase purchaseUseCase;
    private final TopUpUseCase topUpUseCase;
    private final TransactionQueryUseCase transactionQueryUseCase;
    private final BalanceManagementUseCase balanceManagementUseCase;
    private final UserDTO userDTO;

    public TransactionController(PurchaseUseCase purchaseUseCase,
            TopUpUseCase topUpUseCase,
            TransactionQueryUseCase transactionQueryUseCase,
            BalanceManagementUseCase balanceManagementUseCase,
            UserDTO userDTO) {
        this.purchaseUseCase = purchaseUseCase;
        this.topUpUseCase = topUpUseCase;
        this.transactionQueryUseCase = transactionQueryUseCase;
        this.balanceManagementUseCase = balanceManagementUseCase;
        this.userDTO = userDTO;
    }

    @PostMapping("/purchase")
    public ResponseEntity<Map<String, Object>> purchase(@Valid @RequestBody PurchaseRequest request)
            throws InsufficientBalanceException {

        TransactionSummary summary = purchaseUseCase.createPurchase(
                getStoreId(), request.denomId(), request.targetNumber());

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

        UUID storeId = getStoreId();
        topUpUseCase.topUp(storeId, request.amount(), request.description());

        BigDecimal balance = balanceManagementUseCase.getBalance(storeId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Top-up berhasil");
        response.put("balance", balance);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance() {

        UUID storeId = getStoreId();
        BigDecimal balance = balanceManagementUseCase.getBalance(storeId);

        Map<String, Object> response = new HashMap<>();
        response.put("storeId", storeId);
        response.put("balance", balance);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable UUID id) {
        TransactionSummary summary = transactionQueryUseCase.getTransaction(id, getStoreId());
        return ResponseEntity.ok(toDTO(summary));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TransactionDTO>> getTransactionHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<TransactionDTO> page = transactionQueryUseCase.getTransactionHistory(getStoreId(), pageable)
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
}
