package com.satset.catalog.service;

import com.satset.catalog.model.DenomType;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;
import com.satset.catalog.dto.CreateDenomRequest;
import com.satset.catalog.dto.UpdateDenomRequest;
import com.satset.catalog.repository.DenomMetaRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DenomDomainServiceTest {

    @Mock
    private DenomRepository denomRepository;
    @Mock
    private DenomMetaRepository metaRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private DenomDomainService denomService;

    private UUID productId;
    private UUID denomId;
    private Products product;
    private ProductDenoms existingDenom;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        denomId = UUID.randomUUID();

        product = new Products();
        product.setId(productId);
        product.setCode("TELKOMSEL");
        product.setName("Telkomsel");

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
        when(productRepository.findByCode("TELKOMSEL")).thenReturn(Optional.of(product));
        when(denomRepository.findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(productId))
                .thenReturn(List.of(existingDenom));

        List<ProductDenoms> result = denomService.findByProduct("TELKOMSEL");

        assertEquals(1, result.size());
        assertEquals("TLKM5", result.get(0).getCode());
    }

    @Test
    void findByProduct_ProductNotFound_ReturnsEmpty() {
        when(productRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        List<ProductDenoms> result = denomService.findByProduct("UNKNOWN");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByCode_ActiveDenom_ReturnsOptional() {
        when(denomRepository.findByCode("TLKM5")).thenReturn(Optional.of(existingDenom));

        Optional<ProductDenoms> result = denomService.findByCode("TLKM5");

        assertTrue(result.isPresent());
        assertEquals("TLKM5", result.get().getCode());
    }

    @Test
    void findByCode_InactiveDenom_ReturnsEmpty() {
        existingDenom.setActive(false);
        when(denomRepository.findByCode("TLKM5")).thenReturn(Optional.of(existingDenom));

        assertTrue(denomService.findByCode("TLKM5").isEmpty());
    }

    @Test
    void getDenomWithMeta_Found_LoadsMeta() {
        com.satset.catalog.model.ProductDenomMeta meta = new com.satset.catalog.model.ProductDenomMeta();
        meta.setId(UUID.randomUUID());
        when(denomRepository.findByCode("TLKM5")).thenReturn(Optional.of(existingDenom));
        when(metaRepository.findByProductDenomId(denomId)).thenReturn(List.of(meta));

        Optional<ProductDenoms> result = denomService.getDenomWithMeta("TLKM5");

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getMetadata().size());
        verify(metaRepository).findByProductDenomId(denomId);
    }

    @Test
    void getDenomWithMeta_NotFound_ReturnsEmpty() {
        when(denomRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertTrue(denomService.getDenomWithMeta("UNKNOWN").isEmpty());
        verify(metaRepository, never()).findByProductDenomId(any());
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
                false, 100, false, 5);
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
                false, null, true, 1);
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
                false, null, true, 1);
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));
        when(denomRepository.existsByCodeAndIdNot("TLKM10", denomId)).thenReturn(true);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> denomService.update(denomId, req));
        assertEquals("DUPLICATE_CODE", ex.getErrorCode());
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
}
