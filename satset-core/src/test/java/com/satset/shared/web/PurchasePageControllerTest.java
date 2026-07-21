package com.satset.shared.web;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.CategoryType;
import com.satset.catalog.service.category.CategoryDomainService;
import com.satset.shared.dto.UserDTO;
import com.satset.transaction.client.WalletGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void purchasePageLoadsPrepaidAndPostpaidCategories() {
        CategoryDomainService categoryService = Mockito.mock(CategoryDomainService.class);
        WalletGateway walletGateway = Mockito.mock(WalletGateway.class);
        UserDTO userDTO = Mockito.mock(UserDTO.class);
        Category pulsa = new Category();
        Category pasca = new Category();
        Mockito.when(categoryService.findByType(CategoryType.PREPAID)).thenReturn(List.of(pulsa));
        Mockito.when(categoryService.findByType(CategoryType.POSTPAID)).thenReturn(List.of(pasca));
        Mockito.when(userDTO.getWalletId()).thenReturn(null);

        PurchasePageController controller = new PurchasePageController(categoryService, walletGateway, userDTO);
        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.purchasePage(model);

        assertThat(view).isEqualTo("pages/purchase/index");
        @SuppressWarnings("unchecked")
        List<Category> categories = (List<Category>) model.getAttribute("initialCategories");
        assertThat(categories).containsExactly(pulsa, pasca);
    }
}
