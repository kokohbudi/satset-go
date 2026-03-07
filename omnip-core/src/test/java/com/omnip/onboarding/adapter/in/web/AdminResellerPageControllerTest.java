package com.omnip.onboarding.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminResellerPageControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminResellerPageController()).build();
    }

    @Test
    void resellerFormPage_ReturnsViewAndAttributes() throws Exception {
        mockMvc.perform(get("/admin/resellers"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/reseller-form"))
                .andExpect(model().attribute("currentPage", "resellers"))
                .andExpect(model().attribute("breadcrumb", "Tambah Reseller"));
    }
}
