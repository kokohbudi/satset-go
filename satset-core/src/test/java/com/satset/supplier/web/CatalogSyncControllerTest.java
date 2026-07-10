package com.satset.supplier.web;

import com.satset.supplier.model.SyncAction;
import com.satset.supplier.model.SyncPreviewItem;
import com.satset.supplier.service.CatalogSyncService;
import com.satset.supplier.service.SyncResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CatalogSyncControllerTest {

    @Mock CatalogSyncService sync;
    private MockMvc mockMvc() { return MockMvcBuilders.standaloneSetup(new CatalogSyncController(sync)).build(); }

    @Test void previewCategories_returnsItems() throws Exception {
        when(sync.previewCategories()).thenReturn(List.of(new SyncPreviewItem(SyncAction.ADD, "E-Money", "E-Money", null)));
        mockMvc().perform(get("/api/admin/catalog/sync/categories/preview"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].key").value("E-Money"));
    }
    @Test void applyCategories_passesSelectedKeys() throws Exception {
        when(sync.applyCategories(List.of("E-Money"))).thenReturn(new SyncResult(1,0,0,0,0));
        mockMvc().perform(post("/api/admin/catalog/sync/categories")
                        .contentType(MediaType.APPLICATION_JSON).content("[\"E-Money\"]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.added").value(1));
        verify(sync).applyCategories(List.of("E-Money"));
    }
    @Test void previewProducts_passesCategoryId() throws Exception {
        UUID id = UUID.randomUUID();
        when(sync.previewProducts(id)).thenReturn(List.of());
        mockMvc().perform(get("/api/admin/catalog/categories/" + id + "/sync/products/preview"))
                .andExpect(status().isOk());
    }
    @Test void applyProducts_passesIdAndKeys() throws Exception {
        UUID id = UUID.randomUUID();
        when(sync.applyProducts(eq(id), eq(List.of("XL")))).thenReturn(new SyncResult(1,0,0,0,0));
        mockMvc().perform(post("/api/admin/catalog/categories/" + id + "/sync/products")
                        .contentType(MediaType.APPLICATION_JSON).content("[\"XL\"]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.added").value(1));
    }
    @Test void applyDenoms_passesIdAndSkus() throws Exception {
        UUID id = UUID.randomUUID();
        when(sync.applyDenoms(eq(id), eq(List.of("x5")))).thenReturn(new SyncResult(1,0,1,0,0));
        mockMvc().perform(post("/api/admin/catalog/products/" + id + "/sync/denoms")
                        .contentType(MediaType.APPLICATION_JSON).content("[\"x5\"]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.deleted").value(1));
    }
    @Test void compare_returnsRows() throws Exception {
        UUID id = UUID.randomUUID();
        when(sync.reconcileForProduct(id)).thenReturn(List.of());
        mockMvc().perform(get("/api/admin/catalog/products/" + id + "/pricelist-compare"))
                .andExpect(status().isOk());
        verify(sync).reconcileForProduct(id);
    }
}
