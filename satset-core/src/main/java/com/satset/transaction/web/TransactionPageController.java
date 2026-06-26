package com.satset.transaction.web;

import com.satset.shared.constant.OmniConstants;
import com.satset.shared.dto.UserDTO;
import com.satset.transaction.service.TransactionDomainService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TransactionPageController {

    private final TransactionDomainService transactionService;
    private final UserDTO userDTO;

    public TransactionPageController(TransactionDomainService transactionService, UserDTO userDTO) {
        this.transactionService = transactionService;
        this.userDTO = userDTO;
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_TRANSACTION + "')")
    public String transactionsPage(Model model) {
        model.addAttribute("currentPage", "transactions");
        model.addAttribute("breadcrumb", "Riwayat Transaksi");
        // SSR first page so client doesn't refetch on render
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<?> history = userDTO.getStoreId() == null
                ? Page.empty(pageable)
                : transactionService.getTransactionHistory(userDTO.getStoreId(), pageable);
        model.addAttribute("initialHistory", history);
        return "pages/transactions/index";
    }
}
