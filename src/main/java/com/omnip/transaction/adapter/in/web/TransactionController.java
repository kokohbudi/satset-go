package com.omnip.transaction.adapter.in.web;

import com.omnip.transaction.adapter.in.web.dto.TransactionDTO;
import com.omnip.shared.dto.UserDTO;
import com.omnip.transaction.adapter.in.web.dto.PurchaseRequest;
import com.omnip.transaction.adapter.in.web.dto.TopUpRequest;
import com.omnip.catalog.domain.model.ProductDenoms;
import com.omnip.transaction.domain.model.Transactions;
import com.omnip.transaction.domain.port.in.BalanceManagementUseCase;
import com.omnip.transaction.domain.port.in.PurchaseUseCase;
import com.omnip.transaction.domain.port.in.TopUpUseCase;
import com.omnip.transaction.domain.port.in.TransactionQueryUseCase;
import com.omnip.shared.exception.InsufficientBalanceException;
import com.omnip.shared.exception.ResourceNotFoundException;
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

        Transactions transaction = purchaseUseCase.createPurchase(
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
        Transactions tx = transactionQueryUseCase.getTransaction(id, getStoreId());
        return ResponseEntity.ok(toDTO(tx));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TransactionDTO>> getTransactionHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<TransactionDTO> page = transactionQueryUseCase.getTransactionHistory(getStoreId(), pageable)
                .map(this::toDTO);
        return ResponseEntity.ok(page);
    }

    // ==================== Mappers ====================

    private TransactionDTO toDTO(Transactions tx) {
        ProductDenoms denom = tx.getProductDenom();
        return new TransactionDTO(
                tx.getId(),
                tx.getStore().getId(),
                tx.getTargetNumber(),
                denom.getName(),
                denom.getProduct() != null ? denom.getProduct().getName() : null,
                tx.getPrice(),
                tx.getAdminFee(),
                tx.getTotal(),
                tx.getStatus(),
                tx.getProviderRef(),
                tx.getSerialNumber(),
                tx.getCreatedAt());
    }

    private UUID getStoreId() {
        if (userDTO.getStores() == null) {
            throw new ResourceNotFoundException("Store", "current user has no store");
        }
        return userDTO.getStores().getId();
    }
}
