package com.satset.catalog.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.satset.catalog.dto.PriceUpdateResult;
import com.satset.catalog.model.Category;
import com.satset.catalog.model.CategoryType;
import com.satset.catalog.model.DenomType;
import com.satset.catalog.model.Products;
import com.satset.catalog.service.CategoryDomainService;
import com.satset.catalog.service.DenomDomainService;
import com.satset.catalog.service.ProductDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminCatalogControllerTest {

    @Mock private CategoryDomainService manageCategoriesUseCase;
    @Mock private ProductDomainService manageProductsUseCase;
    @Mock private DenomDomainService manageDenomsUseCase;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AdminCatalogController controller = new AdminCatalogController(
                manageCategoriesUseCase, manageProductsUseCase, manageDenomsUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ==================== Category ====================

    @Test
    void listCategory_ReturnsOk_WithMappedDTOs() throws Exception {
        Category cat = buildCategory("PULSA", "Pulsa");

        when(manageCategoriesUseCase.findAllForAdmin()).thenReturn(List.of(cat));

        mockMvc.perform(get("/api/admin/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("PULSA"))
                .andExpect(jsonPath("$[0].name").value("Pulsa"));
    }

    @Test
    void listCategory_EmptyList_ReturnsOkWithEmptyArray() throws Exception {
        when(manageCategoriesUseCase.findAllForAdmin()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getCategory_Found_ReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        Category cat = buildCategory("PULSA", "Pulsa");
        cat.setId(id);

        when(manageCategoriesUseCase.findById(id)).thenReturn(Optional.of(cat));

        mockMvc.perform(get("/api/admin/catalog/categories/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("PULSA"));
    }

    @Test
    void getCategory_NotFound_Returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(manageCategoriesUseCase.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/catalog/categories/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCategory_ReturnsCreated() throws Exception {
        Category created = buildCategory("GAME", "Game");
        when(manageCategoriesUseCase.create(any())).thenReturn(created);

        String body = """
                {"code":"GAME","name":"Game","categoryType":"PREPAID","active":true,"sortOrder":1}
                """;

        mockMvc.perform(post("/api/admin/catalog/categories")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("GAME"));
    }

    @Test
    void updateCategory_ReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        Category updated = buildCategory("GAME", "Game Updated");
        updated.setId(id);
        when(manageCategoriesUseCase.update(any(), any())).thenReturn(updated);

        String body = """
                {"code":"GAME","name":"Game Updated","categoryType":"PREPAID","active":true,"sortOrder":1}
                """;

        mockMvc.perform(put("/api/admin/catalog/categories/{id}", id)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Game Updated"));
    }

    @Test
    void deleteCategory_ReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/catalog/categories/{id}", id))
                .andExpect(status().isNoContent());

        verify(manageCategoriesUseCase).softDelete(id);
    }

    // ==================== Products ====================

    @Test
    void listProducts_WithCategoryId_OnlyCallsFindByCategory() throws Exception {
        UUID categoryId = UUID.randomUUID();
        Products product = buildProduct("TEL", "Telkomsel", categoryId);

        when(manageProductsUseCase.findByCategoryForAdmin(categoryId)).thenReturn(List.of(product));

        mockMvc.perform(get("/api/admin/catalog/products")
                        .param("categoryId", categoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TEL"));

        verify(manageProductsUseCase).findByCategoryForAdmin(categoryId);
        // should NOT call findAllForAdmin when categoryId is provided
        verify(manageCategoriesUseCase, org.mockito.Mockito.never()).findAllForAdmin();
    }

    @Test
    void listProducts_WithoutCategoryId_ReturnsAllProducts() throws Exception {
        UUID catId1 = UUID.randomUUID();
        UUID catId2 = UUID.randomUUID();

        when(manageProductsUseCase.findAllForAdmin()).thenReturn(List.of(
                buildProduct("TEL", "Telkomsel", catId1),
                buildProduct("XL", "XL Axiata", catId2)));

        mockMvc.perform(get("/api/admin/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(manageProductsUseCase).findAllForAdmin();
    }

    @Test
    void getProduct_Found_ReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        Products product = buildProduct("TEL", "Telkomsel", UUID.randomUUID());
        product.setId(id);

        when(manageProductsUseCase.findById(id)).thenReturn(Optional.of(product));

        mockMvc.perform(get("/api/admin/catalog/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getProduct_NotFound_Returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(manageProductsUseCase.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/catalog/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_ReturnsCreated() throws Exception {
        UUID catId = UUID.randomUUID();
        Products created = buildProduct("XL", "XL Axiata", catId);
        when(manageProductsUseCase.create(any())).thenReturn(created);

        String body = String.format("""
                {"categoryId":"%s","code":"XL","name":"XL Axiata","active":true,"sortOrder":1}
                """, catId);

        mockMvc.perform(post("/api/admin/catalog/products")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("XL"));
    }

    @Test
    void updateProduct_ReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        Products updated = buildProduct("XL", "XL Updated", catId);
        updated.setId(id);
        when(manageProductsUseCase.update(any(), any())).thenReturn(updated);

        String body = String.format("""
                {"categoryId":"%s","code":"XL","name":"XL Updated","active":true,"sortOrder":1}
                """, catId);

        mockMvc.perform(put("/api/admin/catalog/products/{id}", id)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("XL Updated"));
    }

    @Test
    void deleteProduct_ReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/catalog/products/{id}", id))
                .andExpect(status().isNoContent());

        verify(manageProductsUseCase).softDelete(id);
    }

    // ==================== Denoms ====================

    @Test
    void listDenoms_ReturnsOk() throws Exception {
        UUID productId = UUID.randomUUID();
        when(manageDenomsUseCase.findByProductForAdmin(productId)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/catalog/products/{productId}/denoms", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // worktree: denom-centric aggregate (ProductDenomDTO) at /denoms
    @Test
    void listAllDenoms_returnsAll() throws Exception {
        com.satset.catalog.model.ProductDenoms d = new com.satset.catalog.model.ProductDenoms();
        d.setId(UUID.randomUUID());
        d.setCode("TSEL5");
        d.setName("Telkomsel 5rb");
        d.setDenomType(com.satset.catalog.model.DenomType.FIXED_DENOM);
        d.setProductId(UUID.randomUUID());
        when(manageDenomsUseCase.findAllForAdmin()).thenReturn(List.of(d));

        mockMvc.perform(get("/api/admin/catalog/denoms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TSEL5"));
    }

    @Test
    void getDenom_Found_ReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        com.satset.catalog.model.ProductDenoms denom = buildDenom(id, productId);
        when(manageDenomsUseCase.findById(id)).thenReturn(Optional.of(denom));

        mockMvc.perform(get("/api/admin/catalog/denoms/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TEL10K"));
    }

    @Test
    void getDenom_NotFound_Returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(manageDenomsUseCase.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/catalog/denoms/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void createDenom_ReturnsCreated() throws Exception {
        UUID productId = UUID.randomUUID();
        com.satset.catalog.model.ProductDenoms created = buildDenom(UUID.randomUUID(), productId);
        when(manageDenomsUseCase.create(any(), any())).thenReturn(created);

        String body = """
                {"code":"TEL10K","name":"Pulsa 10K","denomType":"FIXED_DENOM","price":10000,"requiresInquiry":false,"active":true,"sortOrder":1}
                """;

        mockMvc.perform(post("/api/admin/catalog/products/{productId}/denoms", productId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TEL10K"));
    }

    @Test
    void updateDenom_ReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        com.satset.catalog.model.ProductDenoms updated = buildDenom(id, productId);
        when(manageDenomsUseCase.update(any(), any())).thenReturn(updated);

        String body = """
                {"code":"TEL10K","name":"Pulsa 10K","denomType":"FIXED_DENOM","price":10000,"requiresInquiry":false,"active":true,"sortOrder":1}
                """;

        mockMvc.perform(put("/api/admin/catalog/denoms/{id}", id)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TEL10K"));
    }

    @Test
    void deleteDenom_ReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/catalog/denoms/{id}", id))
                .andExpect(status().isNoContent());

        verify(manageDenomsUseCase).softDelete(id);
    }

    @Test
    void updateDenomPrices_ReturnsPerItemResults() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(manageDenomsUseCase.updatePrices(any())).thenReturn(List.of(
                PriceUpdateResult.ok(id1, "byu10"),
                PriceUpdateResult.fail(id2, "flash1", "Harga harus > 0")));

        String body = "[{\"id\":\"" + id1 + "\",\"price\":1500},{\"id\":\"" + id2 + "\",\"price\":-1}]";

        mockMvc.perform(put("/api/admin/catalog/denoms/prices")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ok").value(true))
                .andExpect(jsonPath("$[0].code").value("byu10"))
                .andExpect(jsonPath("$[1].ok").value(false))
                .andExpect(jsonPath("$[1].error").value("Harga harus > 0"));
    }

    @Test
    void updateDenomPrices_LiteralRouteWins_NotSingleDenomUpdate() throws Exception {
        // Guard: PUT /denoms/prices TIDAK boleh nyangkut ke PUT /denoms/{id} (UUID parse 400)
        when(manageDenomsUseCase.updatePrices(any())).thenReturn(List.of());

        mockMvc.perform(put("/api/admin/catalog/denoms/prices")
                        .contentType("application/json").content("[]"))
                .andExpect(status().isOk());

        verify(manageDenomsUseCase).updatePrices(any());
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

    private Products buildProduct(String code, String name, UUID categoryId) {
        Category category = new Category();
        category.setId(categoryId);
        category.setCode("CAT");
        category.setName("Category");

        Products product = new Products();
        product.setId(UUID.randomUUID());
        product.setCode(code);
        product.setName(name);
        product.setCategoryId(categoryId);
        product.setActive(true);
        return product;
    }

    private com.satset.catalog.model.ProductDenoms buildDenom(UUID id, UUID productId) {
        Products product = new Products();
        product.setId(productId);
        product.setCode("TEL");
        product.setName("Telkomsel");

        com.satset.catalog.model.ProductDenoms denom = new com.satset.catalog.model.ProductDenoms();
        denom.setId(id);
        denom.setCode("TEL10K");
        denom.setName("Pulsa 10K");
        denom.setPrice(new java.math.BigDecimal("10000"));
        denom.setActive(true);
        denom.setProductId(productId);
        return denom;
    }
}
