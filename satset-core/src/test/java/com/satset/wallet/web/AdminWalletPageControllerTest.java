package com.satset.wallet.web;

import com.satset.onboarding.model.Stores;
import com.satset.onboarding.repository.StoreRepository;
import com.satset.wallet.model.WalletAccountEntity;
import com.satset.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AdminWalletPageControllerTest {

    @Mock
    private WalletService walletService;
    @Mock
    private StoreRepository storeRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdminWalletPageController(walletService, storeRepository)).build();
    }

    @Test
    void adjustPage_renders_table_view_with_wallets_correlated_to_store_email() throws Exception {
        WalletAccountEntity acc = WalletAccountEntity.newAccount("7000000001");
        acc.setBalance(new BigDecimal("150000"));
        when(walletService.listAccounts()).thenReturn(List.of(acc));

        Stores store = new Stores();
        store.setWalletId("7000000001");
        store.setName("Toko Jaya");
        store.setEmail("owner@tokojaya.com");
        when(storeRepository.findByWalletIdIn(List.of("7000000001"))).thenReturn(List.of(store));

        mockMvc.perform(get("/admin/wallets/adjust"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/wallet-adjust"))
                .andExpect(model().attribute("currentPage", "wallets"))
                .andExpect(model().attributeExists("initialWallets"));
    }
}
