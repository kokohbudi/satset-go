package com.satset.catalog.adapter.in.web;

import com.satset.catalog.domain.model.Category;
import com.satset.catalog.domain.model.CategoryType;
import com.satset.catalog.domain.model.ProductDenoms;
import com.satset.catalog.domain.model.Products;
import com.satset.catalog.domain.port.in.BrowseCategoriesUseCase;
import com.satset.catalog.domain.port.in.BrowseDenomsUseCase;
import com.satset.catalog.domain.port.in.BrowseProductsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductCatalogControllerTest {

    @Mock private BrowseCategoriesUseCase browseCategoriesUseCase;
    @Mock private BrowseProductsUseCase browseProductsUseCase;
    @Mock private BrowseDenomsUseCase browseDenomsUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductCatalogController controller = new ProductCatalogController(
                browseCategoriesUseCase, browseProductsUseCase, browseDenomsUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ==================== Category ====================

    @Test
    void getAllCategory_ReturnsOk_WithList() throws Exception {
        when(browseCategoriesUseCase.findAll()).thenReturn(List.of(buildCategory("PULSA", "Pulsa")));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("PULSA"))
                .andExpect(jsonPath("$[0].name").value("Pulsa"));
    }

    @Test
    void getCategoryByType_ReturnsOk() throws Exception {
        when(browseCategoriesUseCase.findByType(CategoryType.PREPAID))
                .thenReturn(List.of(buildCategory("PULSA", "Pulsa")));

        mockMvc.perform(get("/api/categories/type/PREPAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getCategoryByCode_Found_ReturnsOk() throws Exception {
        when(browseCategoriesUseCase.findByCode("PULSA"))
                .thenReturn(Optional.of(buildCategory("PULSA", "Pulsa")));

        mockMvc.perform(get("/api/categories/PULSA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PULSA"));
    }

    @Test
    void getCategoryByCode_NotFound_Returns404() throws Exception {
        when(browseCategoriesUseCase.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/categories/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    // ==================== Products ====================

    @Test
    void getProductsByCategory_ReturnsOk_WithList() throws Exception {
        when(browseProductsUseCase.findByCategory("PULSA"))
                .thenReturn(List.of(buildProduct("TEL", "Telkomsel")));

        mockMvc.perform(get("/api/categories/PULSA/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TEL"));
    }

    @Test
    void getProductByCode_Found_ReturnsOk() throws Exception {
        when(browseProductsUseCase.findByCode("TEL"))
                .thenReturn(Optional.of(buildProduct("TEL", "Telkomsel")));

        mockMvc.perform(get("/api/products/TEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Telkomsel"));
    }

    @Test
    void getProductByCode_NotFound_Returns404() throws Exception {
        when(browseProductsUseCase.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    // ==================== Denoms ====================

    @Test
    void getDenomsByProduct_ReturnsOk_WithList() throws Exception {
        when(browseDenomsUseCase.findByProduct("TEL"))
                .thenReturn(List.of(buildDenom("TEL10K", "Pulsa 10K")));

        mockMvc.perform(get("/api/products/TEL/denoms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TEL10K"));
    }

    @Test
    void getDenomByCode_Found_ReturnsOk() throws Exception {
        when(browseDenomsUseCase.getDenomWithMeta("TEL10K"))
                .thenReturn(Optional.of(buildDenom("TEL10K", "Pulsa 10K")));

        mockMvc.perform(get("/api/denoms/TEL10K"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pulsa 10K"));
    }

    @Test
    void getDenomByCode_NotFound_Returns404() throws Exception {
        when(browseDenomsUseCase.getDenomWithMeta("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/denoms/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    // ==================== Helpers ====================

    private Category buildCategory(String code, String name) {
        Category cat = new Category();
        cat.setId(UUID.randomUUID());
        cat.setCode(code);
        cat.setName(name);
        cat.setCategoryType(CategoryType.PREPAID);
        cat.setActive(true);
        return cat;
    }

    private Products buildProduct(String code, String name) {
        Products p = new Products();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setName(name);
        p.setActive(true);
        return p;
    }

    private ProductDenoms buildDenom(String code, String name) {
        ProductDenoms d = new ProductDenoms();
        d.setId(UUID.randomUUID());
        d.setCode(code);
        d.setName(name);
        d.setPrice(new BigDecimal("10000"));
        d.setActive(true);
        return d;
    }
}
