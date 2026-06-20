package com.satset.transaction.adapter.out.wallet;

import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.transaction.adapter.out.wallet.dto.WalletBalanceResponse;
import com.satset.transaction.adapter.out.wallet.dto.WalletMutationRequest;
import com.satset.transaction.adapter.out.wallet.dto.WalletMutationResponse;
import com.satset.transaction.adapter.out.wallet.dto.WalletRefundRequest;
import com.satset.transaction.domain.model.MutationReferenceType;
import com.satset.transaction.domain.model.MutationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Sole balance provider — all balance operations go to the wallet service over HTTP.
 */
@Component
public class WalletClientAdapter {

    private static final Logger log = LoggerFactory.getLogger(WalletClientAdapter.class);

    private final RestClient restClient;

    public WalletClientAdapter(
            @Value("${wallet.base-url}") String baseUrl,
            OAuth2AuthorizedClientManager authorizedClientManager) {

        var interceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        interceptor.setClientRegistrationIdResolver(_ -> "wallet-service");

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(interceptor)
                .build();
    }

    public MutationResult deductBalance(String walletId, BigDecimal amount,
            MutationReferenceType referenceType, UUID referenceId, String description)
            throws InsufficientBalanceException {

        var req = new WalletMutationRequest(walletId, amount, referenceId, "TRANSACTION", description);
        try {
            WalletMutationResponse resp = restClient.post()
                    .uri("/internal/wallet/debit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(WalletMutationResponse.class);
            if (resp == null) throw new ResourceNotFoundException("WalletMutation", walletId);
            log.info("DEBIT via wallet-service wallet={} amount={} balanceAfter={}", walletId, amount, resp.balanceAfter());
            return new MutationResult(resp.id(), resp.balanceAfter());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("WalletAccount", walletId);
            }
            if (e.getStatusCode().value() == 422) {
                throw new InsufficientBalanceException("Saldo tidak mencukupi");
            }
            throw e;
        }
    }

    public MutationResult addBalance(String walletId, BigDecimal amount,
            MutationReferenceType referenceType, UUID referenceId, String description) {

        if (referenceType == MutationReferenceType.REFUND) {
            return callRefund(walletId, amount, referenceId, description);
        }
        var req = new WalletMutationRequest(walletId, amount, referenceId, "TOPUP", description);
        try {
            WalletMutationResponse resp = restClient.post()
                    .uri("/internal/wallet/credit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(WalletMutationResponse.class);
            if (resp == null) throw new ResourceNotFoundException("WalletMutation", walletId);
            log.info("CREDIT via wallet-service wallet={} amount={} balanceAfter={}", walletId, amount, resp.balanceAfter());
            return new MutationResult(resp.id(), resp.balanceAfter());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("WalletAccount", walletId);
            }
            throw e;
        }
    }

    public BigDecimal getBalance(String walletId) {
        try {
            WalletBalanceResponse resp = restClient.get()
                    .uri("/internal/wallet/balance/{walletId}", walletId)
                    .retrieve()
                    .body(WalletBalanceResponse.class);
            if (resp == null) throw new ResourceNotFoundException("WalletAccount", walletId);
            return resp.balance();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("WalletAccount", walletId);
            }
            throw e;
        }
    }

    private MutationResult callRefund(String walletId, BigDecimal amount, UUID originalReferenceId, String description) {
        var req = new WalletRefundRequest(walletId, amount, originalReferenceId, description);
        try {
            WalletMutationResponse resp = restClient.post()
                    .uri("/internal/wallet/refund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(WalletMutationResponse.class);
            if (resp == null) throw new ResourceNotFoundException("WalletMutation", walletId);
            log.info("REFUND via wallet-service wallet={} amount={} balanceAfter={}", walletId, amount, resp.balanceAfter());
            return new MutationResult(resp.id(), resp.balanceAfter());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("WalletAccount", walletId);
            }
            throw e;
        }
    }
}
