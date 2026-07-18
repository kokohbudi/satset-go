package com.satset.shared.web;

import com.satset.onboarding.repository.StoreRepository;
import com.satset.quickmenu.service.menu.QuickMenuService;
import com.satset.shared.dto.UserDTO;
import com.satset.transaction.client.WalletGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserDTO userDTO = new UserDTO(); // no wallet -> formatBalance returns "Rp 0"
        WalletGateway walletGateway = org.mockito.Mockito.mock(WalletGateway.class);
        StoreRepository storeRepository = org.mockito.Mockito.mock(StoreRepository.class);
        QuickMenuService quickMenuService = org.mockito.Mockito.mock(QuickMenuService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardController(walletGateway, userDTO, storeRepository, quickMenuService)).build();
    }

    @Test
    void landingPage_Authenticated_RedirectsToDashboard() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("alice", "creds", List.of());

        mockMvc.perform(get("/").principal(auth))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void landingPage_AnonymousUser_ReturnsLandingPage() throws Exception {
        var auth = new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        mockMvc.perform(get("/").principal(auth))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"));
    }

    @Test
    void landingPage_NoAuth_ReturnsLandingPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"));
    }

    @Test
    void dashboard_ReturnsViewWithAttributes() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/dashboard/index"))
                .andExpect(model().attribute("currentPage", "dashboard"))
                .andExpect(model().attribute("breadcrumb", "Dashboard"));
    }
}
