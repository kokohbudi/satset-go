package com.omnip.controllers;

import com.omnip.dtos.TransactionDTO;
import com.omnip.dtos.UserDTO;
import com.omnip.dtos.requests.PurchaseRequest;
import com.omnip.dtos.requests.TopUpRequest;
import com.omnip.entities.Transactions;
import com.omnip.exceptions.InsufficientBalanceException;
import com.omnip.exceptions.ResourceNotFoundException;
import com.omnip.services.BalanceService;
import com.omnip.services.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final BalanceService balanceService;
    private final UserDTO userDTO;

    public TransactionController(TransactionService transactionService,
            BalanceService balanceService,
            UserDTO userDTO) {
        this.transactionService = transactionService;
        this.balanceService = balanceService;
        this.userDTO = userDTO;
    }

    @PostMapping("/purchase")
    public ResponseEntity<Map<String, Object>> purchase(@Valid @RequestBody PurchaseRequest request)
            throws InsufficientBalanceException {

        Transactions transaction = transactionService.createPurchase(
                getStoreId(), request.denomId(), request.targetNumber());

        Map<String, Object> response = new HashMap<>();
        response.put("status", transaction.getStatus().name());
        response.put("transactionId", transaction.getId());
        response.put("total", transaction.getTotal());
        response.put("providerRef", transaction.getProviderRef());
        response.put("serialNumber", transaction.getSerialNumber());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/topup")
    public ResponseEntity<Map<String, Object>> topUp(@Valid @RequestBody TopUpRequest request) {

        UUID storeId = getStoreId();
        transactionService.topUp(storeId, request.amount(), request.description());

        BigDecimal balance = balanceService.getBalance(storeId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Top-up berhasil");
        response.put("balance", balance);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance() {

        UUID storeId = getStoreId();
        BigDecimal balance = balanceService.getBalance(storeId);

        Map<String, Object> response = new HashMap<>();
        response.put("storeId", storeId);
        response.put("balance", balance);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransaction(id, getStoreId()));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TransactionDTO>> getTransactionHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(getStoreId(), pageable));
    }

    private UUID getStoreId() {
        if (userDTO.getStores() == null) {
            throw new ResourceNotFoundException("Store", "current user has no store");
        }
        return userDTO.getStores().getId();
    }
}
