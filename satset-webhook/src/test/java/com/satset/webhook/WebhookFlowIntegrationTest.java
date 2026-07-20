package com.satset.webhook;

import com.satset.catalog.model.DenomType;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.repository.DenomRepository;
import com.satset.transaction.model.TransactionStatus;
import com.satset.transaction.model.Transactions;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.wallet.model.WalletAccountEntity;
import com.satset.wallet.repository.WalletAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * First real HTTP -> DB proof for the webhook flow, per
 * docs/superpowers/specs/2026-07-20-webhook-split-deploy-design.md Testing section.
 */
@SpringBootTest(classes = WebhookApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "digiflazz.webhook.secret=" + WebhookFlowIntegrationTest.SECRET)
@AutoConfigureMockMvc
@Testcontainers
class WebhookFlowIntegrationTest {

    static final String SECRET = "test-webhook-secret";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) throws Exception {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Fresh Testcontainers Postgres has no tables yet — prod defaults to "none"
        // (schema already managed by satset-core against the shared Neon DB).
        registry.add("webhook.hibernate.ddl-auto", () -> "update");

        // Hibernate's ddl-auto=update creates tables but not schemas — satset_wallet
        // is pre-provisioned on the real DB, so provision it here too for a fresh container.
        try (var conn = postgres.createConnection("")) {
            conn.createStatement().execute("CREATE SCHEMA IF NOT EXISTS satset_wallet");
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private DenomRepository denomRepository;
    @Autowired
    private WalletAccountRepository walletAccountRepository;

    private UUID denomId;
    private String walletId;

    @BeforeEach
    void seed() {
        ProductDenoms denom = new ProductDenoms();
        denom.setProductId(UUID.randomUUID());
        denom.setCode("TLKM5-" + UUID.randomUUID());
        denom.setName("Telkomsel 5K");
        denom.setDenomType(DenomType.FIXED_DENOM);
        denom.setPrice(new BigDecimal("5000.00"));
        denom.setBasePrice(new BigDecimal("4600.00"));
        denom.setAdminFee(BigDecimal.ZERO);
        denom.setActive(true);
        denom.setDeleted(false);
        denom = denomRepository.save(denom);
        denomId = denom.getId();

        walletId = "70" + System.nanoTime() % 100000000L;
        WalletAccountEntity wallet = new WalletAccountEntity();
        wallet.setWalletId(walletId);
        wallet.setBalance(new BigDecimal("5000.00"));
        walletAccountRepository.save(wallet);
    }

    private Transactions seedTransaction(String refNo, TransactionStatus status) {
        Transactions tx = new Transactions();
        tx.setStoreId(UUID.randomUUID());
        tx.setWalletId(walletId);
        tx.setProductDenomId(denomId);
        tx.setDenomName("Telkomsel 5K");
        tx.setProductName("Telkomsel");
        tx.setTargetNumber("081234567890");
        tx.setPrice(new BigDecimal("5000.00"));
        tx.setAdminFee(BigDecimal.ZERO);
        tx.setTotal(new BigDecimal("5000.00"));
        tx.setStatus(status);
        tx.setRefNo(refNo);
        return transactionRepository.save(tx);
    }

    private int doPost(String body, String signature) throws Exception {
        var request = post("/api/webhooks/digiflazz")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (signature != null) {
            request = request.header("X-Hub-Signature", signature);
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        return result.getResponse().getStatus();
    }

    private static String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return "sha1=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String body(String refNo, String status, String rc, String sn, String price) {
        return """
                {"data":{"ref_id":"%s","customer_no":"081234567890","buyer_sku_code":"TLKM5",
                "message":"%s","status":"%s","rc":"%s","sn":"%s","price":%s}}
                """.formatted(refNo, status, status, rc, sn, price);
    }

    @Test
    void suksesPayload_settlesTransactionToSuccess() throws Exception {
        Transactions tx = seedTransaction("ref-sukses-1", TransactionStatus.PROCESSING);
        String b = body(tx.getRefNo(), "Sukses", "00", "SN-XYZ", "4600");

        int status = doPost(b, sign(b));

        assertThat(status).isEqualTo(HttpStatus.OK.value());
        Transactions reloaded = transactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(reloaded.getSerialNumber()).isEqualTo("SN-XYZ");
        assertThat(reloaded.getProviderRef()).isEqualTo(tx.getRefNo());
        assertThat(reloaded.getCostPrice()).isEqualByComparingTo(new BigDecimal("4600"));
        assertThat(reloaded.getMargin()).isEqualByComparingTo(new BigDecimal("400.00"));
    }

    @Test
    void gagalRc02_refundsWalletAndMarksRefunded() throws Exception {
        Transactions tx = seedTransaction("ref-gagal-1", TransactionStatus.PROCESSING);
        String b = body(tx.getRefNo(), "Gagal", "02", "", "0");

        int status = doPost(b, sign(b));

        assertThat(status).isEqualTo(HttpStatus.OK.value());
        Transactions reloaded = transactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        WalletAccountEntity wallet = walletAccountRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    void gagalRc01_staysProcessing_noRefund() throws Exception {
        Transactions tx = seedTransaction("ref-gagal-pending-1", TransactionStatus.PROCESSING);
        String b = body(tx.getRefNo(), "Gagal", "01", "", "0");

        int status = doPost(b, sign(b));

        assertThat(status).isEqualTo(HttpStatus.OK.value());
        Transactions reloaded = transactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.PROCESSING);
        WalletAccountEntity wallet = walletAccountRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void replayOfSettledTransaction_isNoOp() throws Exception {
        Transactions tx = seedTransaction("ref-replay-1", TransactionStatus.PROCESSING);
        String first = body(tx.getRefNo(), "Sukses", "00", "SN-1", "4600");
        doPost(first, sign(first));

        String replay = body(tx.getRefNo(), "Sukses", "00", "SN-1", "4600");
        int status = doPost(replay, sign(replay));

        assertThat(status).isEqualTo(HttpStatus.OK.value());
        Transactions reloaded = transactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        WalletAccountEntity wallet = walletAccountRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("5000.00")); // not refunded twice
    }

    @Test
    void badSignature_returns401_dbUntouched() throws Exception {
        Transactions tx = seedTransaction("ref-badsig-1", TransactionStatus.PROCESSING);
        String b = body(tx.getRefNo(), "Sukses", "00", "SN-1", "4600");

        int status = doPost(b, "sha1=0000000000000000000000000000000000000000");

        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        Transactions reloaded = transactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.PROCESSING);
    }

    @Test
    void unknownRefId_returns404() throws Exception {
        String b = body("no-such-ref-" + UUID.randomUUID(), "Sukses", "00", "SN-1", "4600");

        int status = doPost(b, sign(b));

        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void malformedJsonWithValidSignature_returns400() throws Exception {
        String malformed = "{not json";

        int status = doPost(malformed, sign(malformed));

        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }
}
