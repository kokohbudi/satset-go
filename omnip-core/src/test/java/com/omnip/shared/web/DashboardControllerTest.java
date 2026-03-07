package com.omnip.shared.web;

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
        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardController()).build();
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
