package com.satset.catalog.web;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.CategoryType;
import com.satset.catalog.service.CategoryDomainService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminCatalogPageControllerTest {

    @Mock private CategoryDomainService manageCategoriesUseCase;
    @Mock private ProductDomainService manageProductsUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdminCatalogPageController(
                        manageCategoriesUseCase, manageProductsUseCase))
                .build();

        when(manageCategoriesUseCase.findAllForAdmin()).thenReturn(List.of(buildCategory()));
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
}
