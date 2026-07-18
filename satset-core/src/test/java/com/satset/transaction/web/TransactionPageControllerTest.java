package com.satset.transaction.web;

import com.satset.shared.dto.UserDTO;
import com.satset.transaction.service.topup.TransactionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransactionPageControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TransactionDomainService transactionService = Mockito.mock(TransactionDomainService.class);
        UserDTO userDTO = new UserDTO();
        mockMvc = MockMvcBuilders.standaloneSetup(new TransactionPageController(transactionService, userDTO)).build();
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
