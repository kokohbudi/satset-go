package com.satset.catalog.web;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.CategoryType;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;
import com.satset.catalog.service.CategoryDomainService;
import com.satset.catalog.service.DenomDomainService;
import com.satset.catalog.service.ProductDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminCatalogPageControllerTest {

    @Mock private CategoryDomainService manageCategoriesUseCase;
    @Mock private ProductDomainService manageProductsUseCase;
    @Mock private DenomDomainService manageDenomsUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdminCatalogPageController(
                        manageCategoriesUseCase, manageProductsUseCase, manageDenomsUseCase))
                .build();

        when(manageCategoriesUseCase.findAllForAdmin()).thenReturn(List.of(buildCategory()));
        when(manageProductsUseCase.findByCategoryForAdmin(any())).thenReturn(List.of());
        when(manageProductsUseCase.findAllForAdmin()).thenReturn(List.of());
    }

    @Test
    void catalogRoot_rendersSinglePage_withSeededData() throws Exception {
        mockMvc.perform(get("/admin/catalog"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/catalog/index"))
                .andExpect(model().attributeExists("initialCategories"))
                .andExpect(model().attributeExists("initialProducts"))
                .andExpect(model().attributeExists("categoryTypes"))
                .andExpect(model().attributeExists("denomTypes"));
    }

    @Test
    void categoriesPage_ReturnsViewAndAttributes() throws Exception {
        mockMvc.perform(get("/admin/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/catalog/categories"))
                .andExpect(model().attribute("currentPage", "admin-catalog"))
                .andExpect(model().attributeExists("initialCategories"))
                .andExpect(model().attributeExists("categoryTypes"));
    }

    @Test
    void productsPage_NoCategoryId_LoadsAllProducts() throws Exception {
        mockMvc.perform(get("/admin/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/catalog/products"))
                .andExpect(model().attributeExists("initialProducts"));
    }

    @Test
    void productsPage_WithCategoryId_FiltersByCategory() throws Exception {
        UUID catId = UUID.randomUUID();
        when(manageProductsUseCase.findByCategoryForAdmin(catId))
                .thenReturn(List.of(buildProduct()));

        mockMvc.perform(get("/admin/catalog/products")
                        .param("categoryId", catId.toString())
                        .param("categoryName", "Pulsa"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("categoryId", catId.toString()))
                .andExpect(model().attribute("categoryName", "Pulsa"));
    }

    @Test
    void denomsPage_ReturnsViewAndInjectsData() throws Exception {
        UUID productId = UUID.randomUUID();
        when(manageDenomsUseCase.findByProductForAdmin(productId)).thenReturn(List.of(buildDenom(productId)));
        when(manageProductsUseCase.findById(productId)).thenReturn(Optional.of(buildProduct()));

        mockMvc.perform(get("/admin/catalog/products/" + productId + "/denoms"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/catalog/denoms"))
                .andExpect(model().attribute("productId", productId))
                .andExpect(model().attributeExists("initialDenoms"))
                .andExpect(model().attributeExists("denomTypes"));
    }

    @Test
    void denomsPage_ProductNotFound_StillReturnsView() throws Exception {
        UUID productId = UUID.randomUUID();
        when(manageDenomsUseCase.findByProductForAdmin(productId)).thenReturn(List.of());
        when(manageProductsUseCase.findById(productId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/catalog/products/" + productId + "/denoms"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/catalog/denoms"));
    }

    // ==================== helpers ====================

    private Category buildCategory() {
        Category c = new Category();
        c.setId(UUID.randomUUID());
        c.setCode("PULSA");
        c.setName("Pulsa");
        c.setCategoryType(CategoryType.PREPAID);
        c.setActive(true);
        return c;
    }

    private Products buildProduct() {
        Products p = new Products();
        p.setId(UUID.randomUUID());
        p.setCode("TEL");
        p.setName("Telkomsel");
        p.setActive(true);
        return p;
    }

    private ProductDenoms buildDenom(UUID productId) {
        ProductDenoms d = new ProductDenoms();
        d.setId(UUID.randomUUID());
        d.setCode("TEL10K");
        d.setName("Pulsa 10K");
        d.setPrice(new BigDecimal("10000"));
        d.setActive(true);
        Products p = buildProduct();
        p.setId(productId);
        d.setProductId(productId);
        return d;
    }
}
