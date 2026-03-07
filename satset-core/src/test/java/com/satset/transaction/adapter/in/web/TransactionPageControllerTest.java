package com.satset.transaction.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransactionPageControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TransactionPageController()).build();
    }

    @Test
    void transactionsPage_ReturnsViewWithAttributes() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/transactions/index"))
                .andExpect(model().attribute("currentPage", "transactions"))
                .andExpect(model().attribute("breadcrumb", "Riwayat Transaksi"));
    }
}
