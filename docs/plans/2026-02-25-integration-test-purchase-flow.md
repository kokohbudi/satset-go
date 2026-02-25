# Integration Test Purchase Flow Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Mengimplementasikan integration test untuk alur Purchase Flow menggunakan `@SpringBootTest` dan `@MockBean` untuk memastikan logika bisnis (happy path, saldo kurang, refund saat provider gagal) berjalan dengan benar dari layer Controller hingga Domain Service tanpa koneksi database asli.

**Architecture:** Menggunakan `@SpringBootTest` dengan `@AutoConfigureMockMvc` untuk menembak endpoint `POST /api/transactions/purchase`. Semua akses ke database dan provider eksternal di-mock menggunakan `@MockBean` (contoh: `TransactionRepositoryPort`, `StoreBalancePort`, `ProviderPort`). Autentikasi disimulasikan melalui `SecurityMockMvcRequestPostProcessors.jwt()` jika diperlukan, atau dengan me-mock ekstraksi principal/user context (misalnya mock komponen yang memberikan storeId dari JWT).

**Tech Stack:** Spring Boot Test, MockMvc, Mockito, JUnit 5.

---

### Task 1: Setup Class Test & Mock Context

**Files:**
- Create: `src/test/java/com/omnip/transaction/adapter/in/web/PurchaseFlowIntegrationTest.java`
- Modify: (None)
- Test: `src/test/java/com/omnip/transaction/adapter/in/web/PurchaseFlowIntegrationTest.java`

**Step 1: Write the failing test setup**
Buat kelas dengan anotasi `@SpringBootTest` dan siapkan semua `@MockBean` yang dibutuhkan berdasarkan dependency di `TransactionController`, `PurchaseUseCase` (via `TransactionDomainService`), dan `BalanceDomainService`. Perlu diingat, arsitektur yang digunakan adalah Hexagonal (Ports & Adapters), jadi yang di-mock kemungkinan adalah *Adapter* (seperti `TransactionJpaRepository`) atau *Port Out* jika controller menginjeksi komponen yang langsung butuh port out. Berdasarkan eksplorasi sebelumnya, test unit `TransactionDomainServiceTest` me-mock `TransactionRepositoryPort`, `StoreMutationRepositoryPort`, `StoreBalancePort`, dan `ProviderPort`. Karena ini adalah integrasi test dari Controller, mock akan dipasang pada level Bean yang terdaftar di Spring Context.

*Catatan: Kita perlu mengecek cara auth bekerja di Controller ini (contoh: `@AuthenticationPrincipal Jwt jwt` atau `SecurityContextHolder`). Untuk sekarang, asumsikan kita butuh mensimulasikan token JWT dengan claim `storeId`.*

```java
package com.omnip.transaction.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnip.transaction.adapter.in.web.dto.PurchaseRequest;
import com.omnip.transaction.domain.model.ProviderResponse;
import com.omnip.transaction.domain.model.TransactionStatus;
import com.omnip.transaction.domain.model.Transactions;
import com.omnip.transaction.domain.port.out.ProviderPort;
import com.omnip.transaction.domain.port.out.StoreBalancePort;
import com.omnip.transaction.domain.port.out.StoreMutationRepositoryPort;
import com.omnip.transaction.domain.port.out.TransactionRepositoryPort;
import com.omnip.catalog.domain.port.out.ProductDenomRepositoryPort;
import com.omnip.catalog.domain.model.ProductDenoms;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PurchaseFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Asumsi dependensi dari TransactionDomainService & Controller:
    @MockBean
    private TransactionRepositoryPort transactionRepositoryPort;

    @MockBean
    private StoreBalancePort storeBalancePort;

    @MockBean
    private StoreMutationRepositoryPort storeMutationRepositoryPort;

    @MockBean
    private ProviderPort providerPort;

    @MockBean
    private ProductDenomRepositoryPort productDenomRepositoryPort;

    private UUID storeId;
    private UUID denomId;
    private String targetNumber;
    private PurchaseRequest purchaseRequest;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        denomId = UUID.randomUUID();
        targetNumber = "081234567890";
        purchaseRequest = new PurchaseRequest();
        purchaseRequest.setProductDenomId(denomId);
        purchaseRequest.setTargetNumber(targetNumber);

        // Setup dasar mock yang dibutuhkan semua test
        ProductDenoms denom = new ProductDenoms();
        denom.setId(denomId);
        denom.setCode("PULSA10");
        denom.setPrice(new BigDecimal("10000.00"));
        denom.setActive(true);
        denom.setDeleted(false);

        when(productDenomRepositoryPort.findById(denomId)).thenReturn(Optional.of(denom));

        when(transactionRepositoryPort.existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
                any(), any(), any(), any(), any())).thenReturn(false);

        when(transactionRepositoryPort.save(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });
    }

    @Test
    void testContextLoads() {
        // Hanya untuk memastikan context Spring bangun dengan benar tanpa error
    }
}
```

**Step 2: Run test to verify setup is correct**
Run: `mvn test -Dtest=PurchaseFlowIntegrationTest#testContextLoads`
Expected: PASS (artinya semua Bean terinjeksi dengan benar dan context bisa dimuat)

**Step 3 & 4 (Setup Completed)**
Tidak ada implementasi kode bisnis yang perlu diubah. Context load berhasil.

**Step 5: Commit**
```bash
git add src/test/java/com/omnip/transaction/adapter/in/web/PurchaseFlowIntegrationTest.java
git commit -m "test: add base integration test class for purchase flow"
```

---

### Task 2: Implement Happy Path Test

**Files:**
- Create: (None)
- Modify: `src/test/java/com/omnip/transaction/adapter/in/web/PurchaseFlowIntegrationTest.java`
- Test: `src/test/java/com/omnip/transaction/adapter/in/web/PurchaseFlowIntegrationTest.java`

**Step 1: Write the failing test**

Tambahkan method ini ke dalam `PurchaseFlowIntegrationTest`:

```java
    @Test
    void whenPurchase_withSufficientBalance_andProviderSuccess_thenSuccess() throws Exception {
        // 1. Setup Mock Behavior
        // Saldo awal 100rb
        when(storeBalancePort.getAvailableBalance(storeId)).thenReturn(new BigDecimal("100000.00"));
        // Potong saldo me-return saldo baru (90rb)
        when(storeBalancePort.deductBalance(any(), any(), anyString(), anyString(), any()))
                .thenReturn(new BigDecimal("90000.00"));

        // Provider berhasil
        when(providerPort.sendTransaction(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(new ProviderResponse(true, "REF-123", "SN-123", "Success"));

        // 2. Eksekusi request dengan MockMvc
        // Kita menggunakan SecurityMockMvcRequestPostProcessors.jwt() untuk mem-bypass auth filter
        // dan mensimulasikan payload JWT yang memiliki claim 'storeId'
        mockMvc.perform(post("/api/transactions/purchase")
                .with(jwt().jwt(jwt -> jwt.claim("storeId", storeId.toString())).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(purchaseRequest)))
                .andExpect(status().isOk())
                // Karena kita belum tahu format DTO response pastinya (bisa 'status' atau 'transactionStatus'),
                // test ini mungkin gagal di assertion JSON Path.
                // Jika TransactionDTO direturn, kita cek isinya:
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value(TransactionStatus.SUCCESS.name()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.providerRef").value("REF-123"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.serialNumber").value("SN-123"));

        // 3. Verifikasi interaksi (Opsional tapi direkomendasikan)
        org.mockito.Mockito.verify(storeBalancePort, org.mockito.Mockito.times(1))
                .deductBalance(org.mockito.ArgumentMatchers.eq(storeId), org.mockito.ArgumentMatchers.any(), anyString(), anyString(), org.mockito.ArgumentMatchers.eq(new BigDecimal("10000.00")));
        org.mockito.Mockito.verify(providerPort, org.mockito.Mockito.times(1))
                .sendTransaction(org.mockito.ArgumentMatchers.eq("081234567890"), org.mockito.ArgumentMatchers.eq("PULSA10"), org.mockito.ArgumentMatchers.eq(new BigDecimal("10000.00")));
    }
```

**Step 2: Run test to verify it fails/passes**
Run: `mvn test -Dtest=PurchaseFlowIntegrationTest#whenPurchase_withSufficientBalance_andProviderSuccess_thenSuccess`
Expected: Kemungkinan akan gagal karena *mismatch* di nama field DTO response (misal: JSONPath $.status vs $.transactionStatus) atau nama parameter mock yang kurang pas. Tugas implementator adalah menyesuaikan assertion dengan nama field asli di `TransactionDTO`.

**Step 3: Fix test assertion / Implementasi**
Perbaiki JSONPath berdasarkan struktur `TransactionDTO` yang asli yang direturn oleh controller. Jika error pada mock method name, sesuaikan interface dari `StoreBalancePort` dan `ProviderPort`.

**Step 4: Run test to verify it passes**
Run: `mvn test -Dtest=PurchaseFlowIntegrationTest#whenPurchase_withSufficientBalance_andProviderSuccess_thenSuccess`
Expected: PASS

**Step 5: Commit**
```bash
git add src/test/java/com/omnip/transaction/adapter/in/web/PurchaseFlowIntegrationTest.java
git commit -m "test: add happy path integration test for purchase flow"
```

---

### Task 3: Implement Insufficient Balance Test

**Files:**
- Create: (None)
- Modify: `src/test/java/com/omnip/transaction/adapter/in/web/PurchaseFlowIntegrationTest.java`
- Test: `src/test/java/com/omnip/transaction/adapter/in/web/PurchaseFlowIntegrationTest.java`

**Step 1: Write the failing test**

Tambahkan method ini:

```java
    @Test
    void whenPurchase_withInsufficientBalance_thenReject() throws Exception {
        // 1. Setup Mock Behavior
        // Saldo awal 5rb (kurang dari harga 10rb)
        when(storeBalancePort.getAvailableBalance(storeId)).thenReturn(new BigDecimal("5000.00"));

        // Ketika deduct dicoba, lempar InsufficientBalanceException (sesuai logika asli service)
        // Atau jika service mengecek saldo sebelum deduct, exception dilempar oleh service.
        // Kita mock throw pada deductBalance untuk jaga-jaga:
        when(storeBalancePort.deductBalance(any(), any(), anyString(), anyString(), any()))
                .thenThrow(new com.omnip.shared.exception.InsufficientBalanceException("Saldo tidak cukup"));

        // 2. Eksekusi request
        mockMvc.perform(post("/api/transactions/purchase")
                .with(jwt().jwt(jwt -> jwt.claim("storeId", storeId.toString())).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(purchaseRequest)))
                // Tergantung global exception handler, bisa return 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").exists());

        // 3. Verifikasi interaksi: Provider TIDAK boleh dipanggil
        org.mockito.Mockito.verify(providerPort, org.mockito.Mockito.never())
                .sendTransaction(anyString(), anyString(), any(BigDecimal.class));
    }
```

**Step 2: Run test to verify it fails/passes**
Run: `mvn test -Dtest=PurchaseFlowIntegrationTest#whenPurchase_withInsufficientBalance_thenReject`
Expected: Kemungkinan gagal jika Exception Handler me-return status code yang berbeda (misal 500 atau 422).

**Step 3: Fix test assertion**
Sesuaikan `status().isBadRequest()` atau JSONPath dengan output error yang dihasilkan aplikasi.

**Step 4: Run test to verify it passes**
Run: `mvn test -Dtest=PurchaseFlowIntegrationTest#whenPurchase_withInsufficientBalance_thenReject`
Expected: PASS

**Step 5: Commit**
```bash
git add src/test/java/com/omnip/transaction/adapter/in/web/PurchaseFlowIntegrationTest.java
git commit -m "test: add insufficient balance scenario for purchase flow"
```

---

### Task 4: Implement Provider Failure & Refund Test

**Files:**
- Create: (None)
- Modify: `src/test/java/com/omnip/transaction/adapter/in/web/PurchaseFlowIntegrationTest.java`
- Test: `src/test/java/com/omnip/transaction/adapter/in/web/PurchaseFlowIntegrationTest.java`

**Step 1: Write the failing test**

Tambahkan method ini:

```java
    @Test
    void whenPurchase_withProviderFailure_thenRefund() throws Exception {
        // 1. Setup Mock Behavior
        // Saldo awal cukup
        when(storeBalancePort.getAvailableBalance(storeId)).thenReturn(new BigDecimal("100000.00"));
        // Potong saldo
        when(storeBalancePort.deductBalance(any(), any(), anyString(), anyString(), any()))
                .thenReturn(new BigDecimal("90000.00"));

        // Provider GAGAL
        when(providerPort.sendTransaction(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(new ProviderResponse(false, null, null, "Timeout Biller"));

        // Refund saldo
        when(storeBalancePort.refundBalance(any(), any(), anyString(), anyString(), any()))
                .thenReturn(new BigDecimal("100000.00"));

        // 2. Eksekusi request
        mockMvc.perform(post("/api/transactions/purchase")
                .with(jwt().jwt(jwt -> jwt.claim("storeId", storeId.toString())).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(purchaseRequest)))
                .andExpect(status().isOk())
                // Assert bahwa status transaksi menjadi FAILED (karena ini response DTO, dan provider FAILED membuat transaksi FAILED)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value(TransactionStatus.FAILED.name()));

        // 3. Verifikasi interaksi: Harus deduct, ke provider, lalu refund
        org.mockito.Mockito.verify(storeBalancePort, org.mockito.Mockito.times(1))
                .deductBalance(org.mockito.ArgumentMatchers.eq(storeId), org.mockito.ArgumentMatchers.any(), anyString(), anyString(), org.mockito.ArgumentMatchers.eq(new BigDecimal("10000.00")));
        org.mockito.Mockito.verify(providerPort, org.mockito.Mockito.times(1))
                .sendTransaction(org.mockito.ArgumentMatchers.eq("081234567890"), org.mockito.ArgumentMatchers.eq("PULSA10"), org.mockito.ArgumentMatchers.eq(new BigDecimal("10000.00")));
        org.mockito.Mockito.verify(storeBalancePort, org.mockito.Mockito.times(1))
                .refundBalance(org.mockito.ArgumentMatchers.eq(storeId), org.mockito.ArgumentMatchers.any(), anyString(), anyString(), org.mockito.ArgumentMatchers.eq(new BigDecimal("10000.00")));
    }
```

**Step 2: Run test to verify it fails/passes**
Run: `mvn test -Dtest=PurchaseFlowIntegrationTest#whenPurchase_withProviderFailure_thenRefund`
Expected: Gagal/Lulus, butuh disesuaikan nama field DTO.

**Step 3: Fix test assertion**
Sesuaikan JSONPath `$.status` menjadi sesuai field di DTO.

**Step 4: Run test to verify it passes**
Run: `mvn test -Dtest=PurchaseFlowIntegrationTest#whenPurchase_withProviderFailure_thenRefund`
Expected: PASS

**Step 5: Commit**
```bash
git add src/test/java/com/omnip/transaction/adapter/in/web/PurchaseFlowIntegrationTest.java
git commit -m "test: add provider failure and refund scenario"
```
