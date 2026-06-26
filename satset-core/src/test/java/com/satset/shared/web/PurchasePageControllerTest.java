package com.satset.shared.web;

import com.satset.catalog.service.CategoryDomainService;
import com.satset.shared.dto.UserDTO;
import com.satset.transaction.client.WalletGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PurchasePageControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CategoryDomainService categoryService = Mockito.mock(CategoryDomainService.class);
        WalletGateway walletGateway = Mockito.mock(WalletGateway.class);
        UserDTO userDTO = new UserDTO();
        mockMvc = MockMvcBuilders.standaloneSetup(new PurchasePageController(categoryService, walletGateway, userDTO)).build();
    }

    @Test
    void purchasePage_ReturnsViewWithAttributes() throws Exception {
        mockMvc.perform(get("/purchase"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/purchase/index"))
                .andExpect(model().attribute("currentPage", "purchase"))
                .andExpect(model().attribute("breadcrumb", "Beli Pulsa"));
    }
}
