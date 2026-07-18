package com.satset.catalog.web;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.CategoryType;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;
import com.satset.catalog.service.category.CategoryDomainService;
import com.satset.catalog.service.denom.DenomDomainService;
import com.satset.catalog.service.product.ProductDomainService;
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

    @Mock private CategoryDomainService browseCategoriesUseCase;
    @Mock private ProductDomainService browseProductsUseCase;
    @Mock private DenomDomainService browseDenomsUseCase;

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
    void getProductByCategoryAndCode_returnsProduct() throws Exception {
        Products p = new Products(); p.setCode("TELKOMSEL"); p.setName("TELKOMSEL");
        when(browseProductsUseCase.findByCategoryAndCode("DATA", "TELKOMSEL"))
                .thenReturn(Optional.of(p));
        mockMvc.perform(get("/api/categories/DATA/products/TELKOMSEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TELKOMSEL"));
    }

    // ==================== Denoms ====================

    @Test
    void getDenomsByCategoryAndProduct_returnsList() throws Exception {
        when(browseDenomsUseCase.findByProduct("DATA", "TELKOMSEL"))
                .thenReturn(List.of(buildDenom("TELKOMSEL10K", "Telkomsel 10K")));
        mockMvc.perform(get("/api/categories/DATA/products/TELKOMSEL/denoms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TELKOMSEL10K"));
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
