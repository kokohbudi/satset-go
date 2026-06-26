package com.satset.transaction.web;

import com.satset.shared.constant.OmniConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TransactionPageController {

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_TRANSACTION + "')")
    public String transactionsPage(Model model) {
        model.addAttribute("currentPage", "transactions");
        model.addAttribute("breadcrumb", "Riwayat Transaksi");
        return "pages/transactions/index";
    }
}
