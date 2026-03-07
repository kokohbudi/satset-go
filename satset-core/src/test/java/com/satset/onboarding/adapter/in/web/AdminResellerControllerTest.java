package com.satset.onboarding.adapter.in.web;

import com.satset.onboarding.domain.port.in.AdminOnboardingUseCase;
import com.satset.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminResellerControllerTest {

    @Mock
    private AdminOnboardingUseCase adminOnboardingUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminResellerController controller = new AdminResellerController(adminOnboardingUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createReseller_Success_Returns200() throws Exception {
        doNothing().when(adminOnboardingUseCase).onboardReseller(any(), any(), any(), any(), any());

        mockMvc.perform(post("/api/admin/resellers")
                        .param("username", "alice")
                        .param("email", "alice@mail.com")
                        .param("orgName", "Alice Store")
                        .param("phone", "08123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void createReseller_BusinessException_Returns400() throws Exception {
        doThrow(new BusinessException("Email sudah terdaftar"))
                .when(adminOnboardingUseCase).onboardReseller(any(), any(), any(), any(), any());

        mockMvc.perform(post("/api/admin/resellers")
                        .param("username", "alice")
                        .param("email", "alice@mail.com")
                        .param("orgName", "Alice Store")
                        .param("phone", "08123456789"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void createReseller_UnexpectedException_Returns500() throws Exception {
        doThrow(new RuntimeException("DB error"))
                .when(adminOnboardingUseCase).onboardReseller(any(), any(), any(), any(), any());

        mockMvc.perform(post("/api/admin/resellers")
                        .param("username", "alice")
                        .param("email", "alice@mail.com")
                        .param("orgName", "Alice Store")
                        .param("phone", "08123456789"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void createReseller_WithUpline_PassesUplineToUseCase() throws Exception {
        doNothing().when(adminOnboardingUseCase).onboardReseller(any(), any(), any(), any(), any());

        mockMvc.perform(post("/api/admin/resellers")
                        .param("username", "bob")
                        .param("email", "bob@mail.com")
                        .param("orgName", "Bob Store")
                        .param("phone", "08111111111")
                        .param("upline", "alice-upline-id"))
                .andExpect(status().isOk());

        verify(adminOnboardingUseCase).onboardReseller("bob", "bob@mail.com", "Bob Store", "08111111111", "alice-upline-id");
    }
}
