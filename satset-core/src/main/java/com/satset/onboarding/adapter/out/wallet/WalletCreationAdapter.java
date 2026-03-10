package com.satset.onboarding.adapter.out.wallet;

import com.satset.onboarding.domain.port.out.WalletCreationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * Adapter for creating wallets via Wallet service REST API.
 */
@Component
public class WalletCreationAdapter implements WalletCreationPort {

    private static final Logger log = LoggerFactory.getLogger(WalletCreationAdapter.class);

    private final RestClient restClient;

    public WalletCreationAdapter(
            @Value("${wallet.base-url:http://localhost:8081}") String walletBaseUrl,
            OAuth2AuthorizedClientManager authorizedClientManager) {

        var interceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        interceptor.setClientRegistrationIdResolver(request -> "wallet-service");

        this.restClient = RestClient.builder()
                .baseUrl(walletBaseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(interceptor)
                .build();
    }

    @Override
    public String createWallet(UUID storeId) {
        log.info("Creating wallet for store {} via Wallet API", storeId);

        try {
            WalletCreationRequest request = new WalletCreationRequest(storeId);

            WalletCreationResponse response = restClient.post()
                    .uri("/internal/wallet/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(WalletCreationResponse.class);

            if (response == null || response.walletId() == null) {
                throw new RuntimeException("Empty response from Wallet API");
            }

            log.info("Wallet created successfully: {} for store {}", response.walletId(), storeId);
            return response.walletId();

        } catch (RestClientException e) {
            log.error("Failed to create wallet for store {}: {}", storeId, e.getMessage());
            throw new RuntimeException("Wallet creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Request DTO for wallet creation.
     */
    public record WalletCreationRequest(UUID storeId) {
    }

    /**
     * Response DTO from wallet creation.
     */
    public record WalletCreationResponse(String walletId, UUID storeId) {
    }
}
