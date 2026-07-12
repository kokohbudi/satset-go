package com.satset.catalog.service;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.DenomType;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;
import com.satset.catalog.dto.CreateDenomRequest;
import com.satset.catalog.dto.UpdateDenomRequest;
import com.satset.catalog.dto.BulkNameUpdateRequest;
import com.satset.catalog.dto.BulkPriceUpdateRequest;
import com.satset.catalog.dto.PriceUpdateResult;
import com.satset.catalog.repository.CategoryRepository;
import com.satset.catalog.repository.DenomRepository;
import com.satset.catalog.repository.ProductRepository;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DenomDomainServiceTest {

    @Mock
    private DenomRepository denomRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private DenomDomainService denomService;

    private UUID productId;
    private UUID denomId;
    private UUID categoryId;
    private Products product;
    private Category category;
    private ProductDenoms existingDenom;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        denomId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = new Category();
        category.setId(categoryId);
        category.setCode("PULSA");

        product = new Products();
        product.setId(productId);
        product.setCode("TELKOMSEL");
        product.setName("Telkomsel");
        product.setActive(true);
        product.setDeleted(false);

        existingDenom = new ProductDenoms();
        existingDenom.setId(denomId);
        existingDenom.setProductId(productId);
        existingDenom.setCode("TLKM5");
        existingDenom.setName("Telkomsel 5K");
        existingDenom.setDenomType(DenomType.FIXED_DENOM);
        existingDenom.setPrice(new BigDecimal("5500.00"));
        existingDenom.setActive(true);
        existingDenom.setDeleted(false);
    }

    // === READ / BROWSE ===

    @Test
    void findByProduct_ProductFound_ReturnsDenoms() {
        when(categoryRepository.findByCode("PULSA")).thenReturn(Optional.of(category));
        when(productRepository.findByCategoryIdAndCode(categoryId, "TELKOMSEL")).thenReturn(Optional.of(product));
        when(denomRepository.findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(productId))
                .thenReturn(List.of(existingDenom));

        List<ProductDenoms> result = denomService.findByProduct("PULSA", "TELKOMSEL");

        assertEquals(1, result.size());
        assertEquals("TLKM5", result.get(0).getCode());
    }

    @Test
    void findByProduct_CategoryNotFound_ReturnsEmpty() {
        when(categoryRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        List<ProductDenoms> result = denomService.findByProduct("UNKNOWN", "TELKOMSEL");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByProduct_ProductNotFound_ReturnsEmpty() {
        when(categoryRepository.findByCode("PULSA")).thenReturn(Optional.of(category));
        when(productRepository.findByCategoryIdAndCode(categoryId, "UNKNOWN")).thenReturn(Optional.empty());

        List<ProductDenoms> result = denomService.findByProduct("PULSA", "UNKNOWN");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByProduct_ProductInactive_ReturnsEmpty() {
        product.setActive(false);
        when(categoryRepository.findByCode("PULSA")).thenReturn(Optional.of(category));
        when(productRepository.findByCategoryIdAndCode(categoryId, "TELKOMSEL")).thenReturn(Optional.of(product));

        List<ProductDenoms> result = denomService.findByProduct("PULSA", "TELKOMSEL");

        assertTrue(result.isEmpty());
        verify(denomRepository, never()).findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(any());
    }

    @Test
    void findByProduct_ProductDeleted_ReturnsEmpty() {
        product.setActive(true);
        product.setDeleted(true);
        when(categoryRepository.findByCode("PULSA")).thenReturn(Optional.of(category));
        when(productRepository.findByCategoryIdAndCode(categoryId, "TELKOMSEL")).thenReturn(Optional.of(product));

        List<ProductDenoms> result = denomService.findByProduct("PULSA", "TELKOMSEL");

        assertTrue(result.isEmpty());
        verify(denomRepository, never()).findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(any());
    }

    @Test
    void getDenomWithMeta_Found_ReturnsDenom() {
        when(denomRepository.findByCode("TLKM5")).thenReturn(Optional.of(existingDenom));

        Optional<ProductDenoms> result = denomService.getDenomWithMeta("TLKM5");

        assertTrue(result.isPresent());
        assertEquals("TLKM5", result.get().getCode());
    }

    @Test
    void getDenomWithMeta_NotFound_ReturnsEmpty() {
        when(denomRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertTrue(denomService.getDenomWithMeta("UNKNOWN").isEmpty());
    }

    @Test
    void findByProductForAdmin_ReturnsList() {
        when(denomRepository.findByProductIdOrderBySortOrder(productId))
                .thenReturn(List.of(existingDenom));

        List<ProductDenoms> result = denomService.findByProductForAdmin(productId);

        assertEquals(1, result.size());
    }

    @Test
    void findById_Found_ReturnsOptional() {
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));

        Optional<ProductDenoms> result = denomService.findById(denomId);

        assertTrue(result.isPresent());
        assertEquals(denomId, result.get().getId());
    }

    @Test
    void findById_NotFound_ReturnsEmpty() {
        when(denomRepository.findById(denomId)).thenReturn(Optional.empty());

        assertTrue(denomService.findById(denomId).isEmpty());
    }

    @Test
    void findAllForAdmin_returnsRepositoryOrder() {
        ProductDenoms a = new ProductDenoms();
        a.setCode("A"); a.setSortOrder(0);
        ProductDenoms b = new ProductDenoms();
        b.setCode("B"); b.setSortOrder(1);
        when(denomRepository.findAllByOrderBySortOrder()).thenReturn(List.of(a, b));

        List<ProductDenoms> result = denomService.findAllForAdmin();

        assertThat(result).containsExactly(a, b);
    }

    // === CREATE ===

    @Test
    void create_Success() throws BusinessException {
        // Arrange
        CreateDenomRequest req = new CreateDenomRequest(
                "tlkm10", "Telkomsel 10K", DenomType.FIXED_DENOM,
                new BigDecimal("10000"), new BigDecimal("10500"),
                new BigDecimal("10000"), BigDecimal.ZERO,
                null, null, null, null,
                false, null, true, 2);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(denomRepository.findByCode("TLKM10")).thenReturn(Optional.empty());
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> {
            ProductDenoms d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        // Act
        ProductDenoms result = denomService.create(productId, req);

        // Assert
        assertNotNull(result.getId());
        assertEquals("TLKM10", result.getCode()); // uppercased
        assertEquals("Telkomsel 10K", result.getName());
        assertEquals(DenomType.FIXED_DENOM, result.getDenomType());
        assertEquals(new BigDecimal("10500"), result.getPrice());
        assertEquals(productId, result.getProductId());
        assertTrue(result.isActive());
        assertFalse(result.isDeleted());
    }

    @Test
    void create_BlankCode_GeneratesProductNominal() throws BusinessException {
        CreateDenomRequest req = new CreateDenomRequest(
                "", "Telkomsel 10K", DenomType.FIXED_DENOM,
                new BigDecimal("10000"), new BigDecimal("10500"), null, null,
                null, null, null, null, false, null, true, 1);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(denomRepository.findByCode("TELKOMSEL10000")).thenReturn(Optional.empty());
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDenoms result = denomService.create(productId, req);

        assertEquals("TELKOMSEL10000", result.getCode());
    }

    @Test
    void create_BlankCode_OpenAmount_GeneratesSeq() throws BusinessException {
        CreateDenomRequest req = new CreateDenomRequest(
                null, "Token bebas", DenomType.OPEN_AMOUNT,
                null, new BigDecimal("1"), null, null,
                null, null, null, null, false, null, true, 1);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(denomRepository.findByCode("TELKOMSEL1")).thenReturn(Optional.empty());
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDenoms result = denomService.create(productId, req);

        assertEquals("TELKOMSEL1", result.getCode());
    }

    @Test
    void create_BlankCode_Collision_AppendsNumber() throws BusinessException {
        CreateDenomRequest req = new CreateDenomRequest(
                "", "Telkomsel 10K", DenomType.FIXED_DENOM,
                new BigDecimal("10000"), new BigDecimal("10500"), null, null,
                null, null, null, null, false, null, true, 1);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(denomRepository.findByCode("TELKOMSEL10000")).thenReturn(Optional.of(existingDenom));
        when(denomRepository.findByCode("TELKOMSEL100002")).thenReturn(Optional.empty());
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDenoms result = denomService.create(productId, req);

        assertEquals("TELKOMSEL100002", result.getCode());
    }

    @Test
    void create_ProductNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        UUID unknownProductId = UUID.randomUUID();
        CreateDenomRequest req = new CreateDenomRequest(
                "X", "X", DenomType.FIXED_DENOM,
                null, new BigDecimal("1000"), null, null,
                null, null, null, null,
                false, null, true, 1);
        when(productRepository.findById(unknownProductId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> denomService.create(unknownProductId, req));
        verify(denomRepository, never()).save(any());
    }

    @Test
    void create_DuplicateCode_ThrowsBusinessException() {
        // Arrange
        CreateDenomRequest req = new CreateDenomRequest(
                "TLKM5", "Duplicate", DenomType.FIXED_DENOM,
                null, new BigDecimal("5500"), null, null,
                null, null, null, null,
                false, null, true, 1);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(denomRepository.findByCode("TLKM5")).thenReturn(Optional.of(existingDenom));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> denomService.create(productId, req));
        assertEquals("DUPLICATE_CODE", ex.getErrorCode());
        verify(denomRepository, never()).save(any());
    }

    @Test
    void create_CodeIsUppercasedAndTrimmed() throws BusinessException {
        // Arrange
        CreateDenomRequest req = new CreateDenomRequest(
                "  tlkm25  ", "Telkomsel 25K", DenomType.FIXED_DENOM,
                null, new BigDecimal("26000"), null, null,
                null, null, null, null,
                false, null, true, 3);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(denomRepository.findByCode("TLKM25")).thenReturn(Optional.empty());
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProductDenoms result = denomService.create(productId, req);

        // Assert
        assertEquals("TLKM25", result.getCode());
    }

    // === UPDATE ===

    @Test
    void update_Success() throws BusinessException {
        // Arrange
        UpdateDenomRequest req = new UpdateDenomRequest(
                "TLKM5V2", "Telkomsel 5K V2", DenomType.FIXED_DENOM,
                new BigDecimal("5000"), new BigDecimal("5800"),
                new BigDecimal("5000"), new BigDecimal("500"),
                30, 1024L, null, null,
                false, 100, false, 5, null);
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));
        when(denomRepository.existsByCodeAndIdNot("TLKM5V2", denomId)).thenReturn(false);
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProductDenoms result = denomService.update(denomId, req);

        // Assert
        assertEquals("TLKM5V2", result.getCode());
        assertEquals("Telkomsel 5K V2", result.getName());
        assertEquals(new BigDecimal("5800"), result.getPrice());
        assertEquals(new BigDecimal("500"), result.getAdminFee());
        assertEquals(30, result.getValidityDays());
        assertEquals(1024L, result.getQuotaMb());
        assertEquals(100, result.getStockAvailable());
        assertFalse(result.isActive());
        assertEquals(5, result.getSortOrder());
    }

    @Test
    void update_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        UpdateDenomRequest req = new UpdateDenomRequest(
                "X", "X", DenomType.FIXED_DENOM,
                null, new BigDecimal("1000"), null, null,
                null, null, null, null,
                false, null, true, 1, null);
        when(denomRepository.findById(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> denomService.update(unknownId, req));
        verify(denomRepository, never()).save(any());
    }

    @Test
    void update_DuplicateCode_ThrowsBusinessException() {
        // Arrange — code "TLKM10" already used by another denom
        UpdateDenomRequest req = new UpdateDenomRequest(
                "TLKM10", "Clash", DenomType.FIXED_DENOM,
                null, new BigDecimal("1000"), null, null,
                null, null, null, null,
                false, null, true, 1, null);
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));
        when(denomRepository.existsByCodeAndIdNot("TLKM10", denomId)).thenReturn(true);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> denomService.update(denomId, req));
        assertEquals("DUPLICATE_CODE", ex.getErrorCode());
        verify(denomRepository, never()).save(any());
    }

    @Test
    void update_ReassignsProduct_WhenDifferentProductIdProvided() throws BusinessException {
        UUID newProductId = UUID.randomUUID();
        Products newProduct = new Products();
        newProduct.setId(newProductId);
        UpdateDenomRequest req = new UpdateDenomRequest(
                "TLKM5", "Telkomsel 5K", DenomType.FIXED_DENOM,
                new BigDecimal("5000"), new BigDecimal("5800"), null, null,
                null, null, null, null,
                false, null, true, 5, newProductId);
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));
        when(denomRepository.existsByCodeAndIdNot("TLKM5", denomId)).thenReturn(false);
        when(productRepository.findById(newProductId)).thenReturn(Optional.of(newProduct));
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDenoms result = denomService.update(denomId, req);

        assertEquals(newProductId, result.getProductId());
    }

    @Test
    void update_ReassignToMissingProduct_ThrowsResourceNotFound() {
        UUID missingProductId = UUID.randomUUID();
        UpdateDenomRequest req = new UpdateDenomRequest(
                "TLKM5", "Telkomsel 5K", DenomType.FIXED_DENOM,
                null, new BigDecimal("5000"), null, null,
                null, null, null, null,
                false, null, true, 1, missingProductId);
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));
        when(denomRepository.existsByCodeAndIdNot("TLKM5", denomId)).thenReturn(false);
        when(productRepository.findById(missingProductId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> denomService.update(denomId, req));
        verify(denomRepository, never()).save(any());
    }

    // === SOFT DELETE ===

    @Test
    void softDelete_Success() {
        // Arrange
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        denomService.softDelete(denomId);

        // Assert
        ArgumentCaptor<ProductDenoms> captor = ArgumentCaptor.forClass(ProductDenoms.class);
        verify(denomRepository).save(captor.capture());
        ProductDenoms saved = captor.getValue();
        assertTrue(saved.isDeleted());
        assertFalse(saved.isActive());
    }

    @Test
    void softDelete_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        when(denomRepository.findById(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> denomService.softDelete(unknownId));
        verify(denomRepository, never()).save(any());
    }

    // === BULK PRICE UPDATE ===

    @Test
    void updatePrices_AllValid_UpdatesAndReturnsOk() {
        UUID otherId = UUID.randomUUID();
        ProductDenoms other = new ProductDenoms();
        other.setId(otherId);
        other.setCode("TLKM10");
        other.setPrice(new BigDecimal("10500.00"));
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));
        when(denomRepository.findById(otherId)).thenReturn(Optional.of(other));
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));

        List<PriceUpdateResult> results = denomService.updatePrices(List.of(
                new BulkPriceUpdateRequest(denomId, new BigDecimal("6000")),
                new BulkPriceUpdateRequest(otherId, new BigDecimal("11000"))));

        assertThat(results).hasSize(2).allMatch(PriceUpdateResult::ok);
        assertThat(existingDenom.getPrice()).isEqualByComparingTo("6000");
        assertThat(other.getPrice()).isEqualByComparingTo("11000");
        verify(denomRepository, times(2)).save(any(ProductDenoms.class));
    }

    @Test
    void updatePrices_NotFound_ItemError_OthersSucceed() {
        UUID missingId = UUID.randomUUID();
        when(denomRepository.findById(missingId)).thenReturn(Optional.empty());
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));

        List<PriceUpdateResult> results = denomService.updatePrices(List.of(
                new BulkPriceUpdateRequest(missingId, new BigDecimal("5000")),
                new BulkPriceUpdateRequest(denomId, new BigDecimal("6000"))));

        assertThat(results.get(0).ok()).isFalse();
        assertThat(results.get(0).error()).isEqualTo("Denom tidak ditemukan");
        assertThat(results.get(1).ok()).isTrue();
        verify(denomRepository, times(1)).save(any(ProductDenoms.class));
    }

    @Test
    void updatePrices_NonPositivePrice_ItemError_NoSave() {
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));

        List<PriceUpdateResult> results = denomService.updatePrices(List.of(
                new BulkPriceUpdateRequest(denomId, new BigDecimal("-1")),
                new BulkPriceUpdateRequest(denomId, BigDecimal.ZERO),
                new BulkPriceUpdateRequest(denomId, null)));

        assertThat(results).hasSize(3).noneMatch(PriceUpdateResult::ok);
        assertThat(results).allMatch(r -> "Harga harus > 0".equals(r.error()));
        verify(denomRepository, never()).save(any(ProductDenoms.class));
    }

    @Test
    void updatePrices_DeletedDenom_ItemError_NoSave() {
        existingDenom.setDeleted(true);
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));

        List<PriceUpdateResult> results = denomService.updatePrices(List.of(
                new BulkPriceUpdateRequest(denomId, new BigDecimal("6000"))));

        assertThat(results.get(0).ok()).isFalse();
        assertThat(results.get(0).error()).isEqualTo("Denom sudah dihapus");
        verify(denomRepository, never()).save(any(ProductDenoms.class));
    }

    @Test
    void updatePrices_NullId_ItemError_NoLookup() {
        List<PriceUpdateResult> results = denomService.updatePrices(List.of(
                new BulkPriceUpdateRequest(null, new BigDecimal("6000"))));

        assertThat(results.get(0).ok()).isFalse();
        assertThat(results.get(0).error()).isEqualTo("Denom tidak ditemukan");
        verify(denomRepository, never()).findById(any());
    }

    @Test
    void updatePrices_IsWriteTransactional() throws Exception {
        // Regression guard: class-level @Transactional(readOnly=true) — tanpa override
        // method-level, write tidak ke-flush. WAJIB @Transactional read-write.
        var tx = DenomDomainService.class
                .getMethod("updatePrices", List.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isFalse();
    }

    // === BULK NAME UPDATE ===

    @Test
    void updateNames_AllValid_UpdatesAndReturnsOk() {
        UUID otherId = UUID.randomUUID();
        ProductDenoms other = new ProductDenoms();
        other.setId(otherId);
        other.setCode("TLKM10");
        other.setName("Telkomsel 10K");
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));
        when(denomRepository.findById(otherId)).thenReturn(Optional.of(other));
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));

        List<PriceUpdateResult> results = denomService.updateNames(List.of(
                new BulkNameUpdateRequest(denomId, "Telkomsel 5rb"),
                new BulkNameUpdateRequest(otherId, "  Telkomsel 10rb  ")));

        assertThat(results).hasSize(2).allMatch(PriceUpdateResult::ok);
        assertThat(existingDenom.getName()).isEqualTo("Telkomsel 5rb");
        assertThat(other.getName()).isEqualTo("Telkomsel 10rb"); // trimmed
        verify(denomRepository, times(2)).save(any(ProductDenoms.class));
    }

    @Test
    void updateNames_NotFound_ItemError_OthersSucceed() {
        UUID missingId = UUID.randomUUID();
        when(denomRepository.findById(missingId)).thenReturn(Optional.empty());
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));

        List<PriceUpdateResult> results = denomService.updateNames(List.of(
                new BulkNameUpdateRequest(missingId, "Baru"),
                new BulkNameUpdateRequest(denomId, "Telkomsel Baru")));

        assertThat(results.get(0).ok()).isFalse();
        assertThat(results.get(0).error()).isEqualTo("Denom tidak ditemukan");
        assertThat(results.get(1).ok()).isTrue();
        verify(denomRepository, times(1)).save(any(ProductDenoms.class));
    }

    @Test
    void updateNames_BlankName_ItemError_NoSave() {
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));

        List<PriceUpdateResult> results = denomService.updateNames(List.of(
                new BulkNameUpdateRequest(denomId, "   "),
                new BulkNameUpdateRequest(denomId, null)));

        assertThat(results).hasSize(2).noneMatch(PriceUpdateResult::ok);
        assertThat(results).allMatch(r -> "Nama kosong".equals(r.error()));
        verify(denomRepository, never()).save(any(ProductDenoms.class));
    }

    @Test
    void updateNames_DeletedDenom_ItemError_NoSave() {
        existingDenom.setDeleted(true);
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));

        List<PriceUpdateResult> results = denomService.updateNames(List.of(
                new BulkNameUpdateRequest(denomId, "Telkomsel Baru")));

        assertThat(results.get(0).ok()).isFalse();
        assertThat(results.get(0).error()).isEqualTo("Denom sudah dihapus");
        verify(denomRepository, never()).save(any(ProductDenoms.class));
    }

    @Test
    void updateNames_NullId_ItemError_NoLookup() {
        List<PriceUpdateResult> results = denomService.updateNames(List.of(
                new BulkNameUpdateRequest(null, "Telkomsel")));

        assertThat(results.get(0).ok()).isFalse();
        assertThat(results.get(0).error()).isEqualTo("Denom tidak ditemukan");
        verify(denomRepository, never()).findById(any());
    }

    @Test
    void updateNames_IsWriteTransactional() throws Exception {
        var tx = DenomDomainService.class
                .getMethod("updateNames", List.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isFalse();
    }

    // === SUPPLIER SYNC ===

    @Test
    void createFromSupplier_keepsCodeLowercase() {
        UUID pid = UUID.randomUUID();
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(i -> i.getArgument(0));
        ProductDenoms d = denomService.createFromSupplier(pid, "dana20", "DANA 20.000", new BigDecimal("20500"));
        assertThat(d.getCode()).isEqualTo("dana20");
        assertThat(d.getProductId()).isEqualTo(pid);
        assertThat(d.getBasePrice()).isEqualByComparingTo("20500");
        assertThat(d.getPrice()).isNull();
        assertThat(d.getDenomType()).isEqualTo(DenomType.FIXED_DENOM);
        assertThat(d.isActive()).isTrue();
    }

    @Test
    void createFromSupplier_softDeletedCode_revives() {
        UUID pid = UUID.randomUUID();
        ProductDenoms old = new ProductDenoms();
        old.setId(UUID.randomUUID());
        old.setCode("dana20");
        old.setDeleted(true);
        old.setActive(false);
        when(denomRepository.findByCode("dana20")).thenReturn(Optional.of(old));
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(i -> i.getArgument(0));
        ProductDenoms d = denomService.createFromSupplier(pid, "dana20", "DANA 20.000", new BigDecimal("20500"));
        assertThat(d.getId()).isEqualTo(old.getId()); // row lama di-revive, bukan bikin baru (hindari UNIQUE code)
        assertThat(d.isDeleted()).isFalse();
        assertThat(d.isActive()).isTrue();
        assertThat(d.getBasePrice()).isEqualByComparingTo("20500");
        assertThat(d.getProductId()).isEqualTo(pid);
    }

    @Test
    void updateCostById_setsBasePrice() {
        UUID id = UUID.randomUUID();
        ProductDenoms e = new ProductDenoms();
        e.setId(id);
        e.setBasePrice(new BigDecimal("5000"));
        when(denomRepository.findById(id)).thenReturn(Optional.of(e));
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(i -> i.getArgument(0));
        denomService.updateCostById(id, new BigDecimal("5450"));
        assertThat(e.getBasePrice()).isEqualByComparingTo("5450");
    }
}
