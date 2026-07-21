package com.satset.shared.web;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.CategoryType;
import com.satset.catalog.service.category.CategoryDomainService;
import com.satset.shared.constant.SatsetConstants;
import com.satset.shared.dto.UserDTO;
import com.satset.transaction.client.WalletGateway;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class PurchasePageController {

    private final CategoryDomainService categoryService;
    private final WalletGateway walletGateway;
    private final UserDTO userDTO;

    public PurchasePageController(CategoryDomainService categoryService, WalletGateway walletGateway, UserDTO userDTO) {
        this.categoryService = categoryService;
        this.walletGateway = walletGateway;
        this.userDTO = userDTO;
    }

    @GetMapping("/purchase")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_PURCHASE + "')")
    public String purchasePage(Model model) {
        model.addAttribute("currentPage", "purchase");
        model.addAttribute("breadcrumb", "Beli Pulsa");
        // SSR categories + balance so client doesn't refetch on render
        List<Category> categories = new java.util.ArrayList<>(categoryService.findByType(CategoryType.PREPAID));
        categories.addAll(categoryService.findByType(CategoryType.POSTPAID));
        BigDecimal balance = userDTO.getWalletId() == null
                ? BigDecimal.ZERO
                : walletGateway.getBalance(userDTO.getWalletId());
        model.addAttribute("initialCategories", categories);
        model.addAttribute("initialBalance", balance);
        return "pages/purchase/index";
    }
}
