package com.satset.onboarding.adapter.in.web;

import com.satset.onboarding.domain.port.in.SelfOnboardingUseCase;
import com.satset.shared.constant.OmniConstants;
import com.satset.shared.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OnboardingControllerTest {

    @Mock
    private SelfOnboardingUseCase selfOnboardingUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OnboardingController controller = new OnboardingController(selfOnboardingUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ==================== GET /onboarding ====================

    @Test
    void showOnboardingForm_NoStore_ReturnsOnboardingPage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("hasStore", null);

        mockMvc.perform(get("/onboarding").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/onboarding"));
    }

    @Test
    void showOnboardingForm_HasStore_RedirectsToDashboard() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("hasStore", true);

        mockMvc.perform(get("/onboarding").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    // ==================== POST /onboarding ====================

    @Test
    void processOnboarding_NoUserDTO_RedirectsToLogin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        // no SESSION_userDTO

        mockMvc.perform(post("/onboarding")
                        .session(session)
                        .param("orgName", "My Store")
                        .param("phone", "08123456789"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void processOnboarding_NoProviderUserId_RedirectsToLogin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        UserDTO userDTO = new UserDTO();
        userDTO.setProviderUserId(null); // missing
        session.setAttribute(OmniConstants.SESSION_USER_DTO, userDTO);

        mockMvc.perform(post("/onboarding")
                        .session(session)
                        .param("orgName", "My Store")
                        .param("phone", "08123456789"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void processOnboarding_Success_RedirectsToDashboard() throws Exception {
        MockHttpSession session = new MockHttpSession();
        UserDTO userDTO = new UserDTO();
        userDTO.setProviderUserId("kc-abc");
        session.setAttribute(OmniConstants.SESSION_USER_DTO, userDTO);
        doNothing().when(selfOnboardingUseCase).onboardStore(any(), any(), any());

        mockMvc.perform(post("/onboarding")
                        .session(session)
                        .param("orgName", "My Store")
                        .param("phone", "08123456789"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(selfOnboardingUseCase).onboardStore("kc-abc", "My Store", "08123456789");
    }

    @Test
    void processOnboarding_Exception_RedirectsToOnboarding() throws Exception {
        MockHttpSession session = new MockHttpSession();
        UserDTO userDTO = new UserDTO();
        userDTO.setProviderUserId("kc-abc");
        session.setAttribute(OmniConstants.SESSION_USER_DTO, userDTO);
        doThrow(new RuntimeException("KC error"))
                .when(selfOnboardingUseCase).onboardStore(any(), any(), any());

        mockMvc.perform(post("/onboarding")
                        .session(session)
                        .param("orgName", "My Store")
                        .param("phone", "08123456789"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/onboarding"));
    }
}
