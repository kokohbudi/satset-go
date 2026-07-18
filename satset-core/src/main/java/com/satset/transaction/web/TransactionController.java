package com.satset.transaction.web;

import com.satset.shared.constant.SatsetConstants;
import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.transaction.dto.PurchaseRequest;
import com.satset.transaction.dto.TransactionDTO;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.service.topup.TransactionDomainService;
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
    private final WalletGateway balanceService;
    private final UserDTO userDTO;

    public TransactionController(TransactionDomainService transactionService,
            WalletGateway balanceService,
            UserDTO userDTO) {
        this.transactionService = transactionService;
        this.balanceService = balanceService;
        this.userDTO = userDTO;
    }

    @PostMapping("/purchase")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_PURCHASE + "')")
    public ResponseEntity<Map<String, Object>> purchase(@Valid @RequestBody PurchaseRequest request)
            throws InsufficientBalanceException {

        TransactionDTO transaction = transactionService.createPurchase(
                getStoreId(), getWalletId(), request.denomId(), request.targetNumber());

        Map<String, Object> response = new HashMap<>();
        response.put("status", transaction.status().name());
        response.put("transactionId", transaction.id());
        response.put("refNo", transaction.refNo());
        response.put("total", transaction.total());
        response.put("providerRef", transaction.providerRef());
        response.put("serialNumber", transaction.serialNumber());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_TRANSACTION + "')")
    public ResponseEntity<Map<String, Object>> getBalance() {

        String walletId = getWalletId();
        BigDecimal balance = balanceService.getBalance(walletId);

        return ResponseEntity.ok(Map.of("walletId", walletId, "balance", balance));
    }

    @GetMapping("/mutations")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_TRANSACTION + "')")
    public ResponseEntity<java.util.List<com.satset.wallet.dto.WalletMutationDTO>> mutations() {
        return ResponseEntity.ok(balanceService.listMutations(getWalletId()));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_TRANSACTION + "')")
    public ResponseEntity<Page<TransactionDTO>> getTransactionHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<TransactionDTO> page = transactionService.getTransactionHistory(getStoreId(), pageable);
        return ResponseEntity.ok(page);
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
