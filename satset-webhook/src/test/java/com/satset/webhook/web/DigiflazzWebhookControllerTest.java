package com.satset.webhook.web;

import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.webhook.config.WebhookSecurityConfig;
import com.satset.webhook.security.DigiflazzSignatureVerifier;
import com.satset.webhook.service.DigiflazzWebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DigiflazzWebhookController.class)
@Import(WebhookSecurityConfig.class)
class DigiflazzWebhookControllerTest {

    private static final String VALID_SIG = "sha1=deadbeef";
    private static final String SUKSES_BODY = """
            {"data":{"ref_id":"30467470","customer_no":"0812","buyer_sku_code":"TLKM5",
            "message":"Sukses","status":"Sukses","rc":"00","sn":"SN-1","price":5000}}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DigiflazzSignatureVerifier verifier;

    @MockitoBean
    private DigiflazzWebhookService service;

    @Test
    void validSignatureKnownRef_returns200_verifierSeesExactRawBody() throws Exception {
        when(verifier.verify(SUKSES_BODY, VALID_SIG)).thenReturn(true);
        when(service.handle(any())).thenReturn(DigiflazzWebhookService.HandleResult.SETTLED);

        mockMvc.perform(post("/api/webhooks/digiflazz")
                        .header("X-Hub-Signature", VALID_SIG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUKSES_BODY))
                .andExpect(status().isOk());

        verify(verifier).verify(eq(SUKSES_BODY), eq(VALID_SIG));
    }

    @Test
    void invalidSignature_returns401_serviceNeverInvoked() throws Exception {
        when(verifier.verify(SUKSES_BODY, VALID_SIG)).thenReturn(false);

        mockMvc.perform(post("/api/webhooks/digiflazz")
                        .header("X-Hub-Signature", VALID_SIG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUKSES_BODY))
                .andExpect(status().isUnauthorized());

        verify(service, never()).handle(any());
    }

    @Test
    void validSignatureMalformedJson_returns400() throws Exception {
        String malformed = "{not json";
        when(verifier.verify(malformed, VALID_SIG)).thenReturn(true);

        mockMvc.perform(post("/api/webhooks/digiflazz")
                        .header("X-Hub-Signature", VALID_SIG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformed))
                .andExpect(status().isBadRequest());

        verify(service, never()).handle(any());
    }

    @Test
    void validSignatureUnknownRef_returns404() throws Exception {
        when(verifier.verify(SUKSES_BODY, VALID_SIG)).thenReturn(true);
        when(service.handle(any())).thenThrow(new ResourceNotFoundException("Transaction", "30467470"));

        mockMvc.perform(post("/api/webhooks/digiflazz")
                        .header("X-Hub-Signature", VALID_SIG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUKSES_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void serviceThrowsRuntimeException_returns500() throws Exception {
        when(verifier.verify(SUKSES_BODY, VALID_SIG)).thenReturn(true);
        when(service.handle(any())).thenThrow(new RuntimeException("db down"));

        mockMvc.perform(post("/api/webhooks/digiflazz")
                        .header("X-Hub-Signature", VALID_SIG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUKSES_BODY))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void replayIgnored_returns200() throws Exception {
        when(verifier.verify(SUKSES_BODY, VALID_SIG)).thenReturn(true);
        when(service.handle(any())).thenReturn(DigiflazzWebhookService.HandleResult.REPLAY_IGNORED);

        mockMvc.perform(post("/api/webhooks/digiflazz")
                        .header("X-Hub-Signature", VALID_SIG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUKSES_BODY))
                .andExpect(status().isOk());
    }
}
