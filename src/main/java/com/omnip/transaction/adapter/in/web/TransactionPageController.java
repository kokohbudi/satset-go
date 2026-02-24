package com.omnip.transaction.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TransactionPageController {

    @GetMapping("/transactions")
    public String transactionsPage(Model model) {
        model.addAttribute("currentPage", "transactions");
        model.addAttribute("breadcrumb", "Riwayat Transaksi");
        return "pages/transactions/index";
    }
}
