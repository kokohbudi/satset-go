package com.omnip.shared.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PurchasePageControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PurchasePageController()).build();
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
