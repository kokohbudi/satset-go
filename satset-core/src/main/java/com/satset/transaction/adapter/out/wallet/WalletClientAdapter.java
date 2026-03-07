package com.satset.transaction.adapter.out.wallet;

import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.transaction.adapter.out.wallet.dto.WalletBalanceResponse;
import com.satset.transaction.adapter.out.wallet.dto.WalletMutationRequest;
import com.satset.transaction.adapter.out.wallet.dto.WalletMutationResponse;
import com.satset.transaction.adapter.out.wallet.dto.WalletRefundRequest;
import com.satset.transaction.domain.model.MutationReferenceType;
import com.satset.transaction.domain.model.MutationResult;
import com.satset.transaction.domain.port.in.BalanceManagementUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "wallet.client.enabled", havingValue = "true")
public class WalletClientAdapter implements BalanceManagementUseCase {

    private static final Logger log = LoggerFactory.getLogger(WalletClientAdapter.class);

    private final RestClient restClient;

    public WalletClientAdapter(
            @Value("${wallet.base-url}") String baseUrl,
            OAuth2AuthorizedClientManager authorizedClientManager) {

        var interceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        interceptor.setClientRegistrationIdResolver(request -> "wallet-service");

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(interceptor)
                .build();
    }

    @Override
    public MutationResult deductBalance(UUID storeId, BigDecimal amount,
            MutationReferenceType referenceType, UUID referenceId, String description)
            throws InsufficientBalanceException {

        var req = new WalletMutationRequest(storeId, amount, referenceId, "TRANSACTION", description);
        try {
            WalletMutationResponse resp = restClient.post()
                    .uri("/internal/wallet/debit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(WalletMutationResponse.class);
            log.info("DEBIT via wallet-service store={} amount={} balanceAfter={}", storeId, amount, resp.balanceAfter());
            return new MutationResult(resp.id(), resp.balanceAfter());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("WalletAccount", storeId);
            }
            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                throw new InsufficientBalanceException("Saldo tidak mencukupi");
            }
            throw e;
        }
    }

    @Override
    public MutationResult addBalance(UUID storeId, BigDecimal amount,
            MutationReferenceType referenceType, UUID referenceId, String description) {

        if (referenceType == MutationReferenceType.REFUND) {
            return callRefund(storeId, amount, referenceId, description);
        }
        var req = new WalletMutationRequest(storeId, amount, referenceId, "TOPUP", description);
        try {
            WalletMutationResponse resp = restClient.post()
                    .uri("/internal/wallet/credit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(WalletMutationResponse.class);
            log.info("CREDIT via wallet-service store={} amount={} balanceAfter={}", storeId, amount, resp.balanceAfter());
            return new MutationResult(resp.id(), resp.balanceAfter());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("WalletAccount", storeId);
            }
            throw e;
        }
    }

    @Override
    public BigDecimal getBalance(UUID storeId) {
        try {
            WalletBalanceResponse resp = restClient.get()
                    .uri("/internal/wallet/balance/{storeId}", storeId)
                    .retrieve()
                    .body(WalletBalanceResponse.class);
            return resp.balance();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("WalletAccount", storeId);
            }
            throw e;
        }
    }

    private MutationResult callRefund(UUID storeId, BigDecimal amount, UUID originalReferenceId, String description) {
        var req = new WalletRefundRequest(storeId, amount, originalReferenceId, description);
        try {
            WalletMutationResponse resp = restClient.post()
                    .uri("/internal/wallet/refund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(WalletMutationResponse.class);
            log.info("REFUND via wallet-service store={} amount={} balanceAfter={}", storeId, amount, resp.balanceAfter());
            return new MutationResult(resp.id(), resp.balanceAfter());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("WalletAccount", storeId);
            }
            throw e;
        }
    }
}
