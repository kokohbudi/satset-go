package com.omnip.catalog.domain.service;

import com.omnip.catalog.domain.model.Categories;
import com.omnip.catalog.domain.model.CategoryType;
import com.omnip.catalog.domain.model.ProductDenoms;
import com.omnip.catalog.domain.model.Products;
import com.omnip.catalog.domain.port.in.CreateProductRequest;
import com.omnip.catalog.domain.port.in.UpdateProductRequest;
import com.omnip.catalog.domain.port.out.CategoryRepositoryPort;
import com.omnip.catalog.domain.port.out.DenomRepositoryPort;
import com.omnip.catalog.domain.port.out.ProductRepositoryPort;
import com.omnip.shared.exception.BusinessException;
import com.omnip.shared.exception.ResourceNotFoundException;
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
class ProductDomainServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;
    @Mock
    private CategoryRepositoryPort categoryRepository;
    @Mock
    private DenomRepositoryPort denomRepository;

    @InjectMocks
    private ProductDomainService productService;

    private UUID categoryId;
    private UUID productId;
    private Categories category;
    private Products existingProduct;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        productId = UUID.randomUUID();

        category = new Categories();
        category.setId(categoryId);
        category.setCode("PULSA");
        category.setName("Pulsa");
        category.setCategoryType(CategoryType.PREPAID);

        existingProduct = new Products();
        existingProduct.setId(productId);
        existingProduct.setCategory(category);
        existingProduct.setCode("TELKOMSEL");
        existingProduct.setName("Telkomsel");
        existingProduct.setActive(true);
        existingProduct.setDeleted(false);
    }

    // === READ / BROWSE ===

    @Test
    void findByCategory_CategoryFound_ReturnsProducts() {
        when(categoryRepository.findByCode("PULSA")).thenReturn(Optional.of(category));
        when(productRepository.findByCategoryIdAndActiveTrueAndDeletedFalseOrderBySortOrder(categoryId))
                .thenReturn(List.of(existingProduct));

        List<Products> result = productService.findByCategory("PULSA");

        assertEquals(1, result.size());
        assertEquals("TELKOMSEL", result.get(0).getCode());
    }

    @Test
    void findByCategory_CategoryNotFound_ReturnsEmpty() {
        when(categoryRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        List<Products> result = productService.findByCategory("UNKNOWN");

        assertTrue(result.isEmpty());
    }

    @Test
    void findActiveProducts_ReturnsList() {
        when(productRepository.findByActiveTrueAndDeletedFalseOrderBySortOrder())
                .thenReturn(List.of(existingProduct));

        List<Products> result = productService.findActiveProducts();

        assertEquals(1, result.size());
    }

    @Test
    void findByCode_ActiveProduct_ReturnsOptional() {
        when(productRepository.findByCode("TELKOMSEL")).thenReturn(Optional.of(existingProduct));

        Optional<Products> result = productService.findByCode("TELKOMSEL");

        assertTrue(result.isPresent());
        assertEquals("TELKOMSEL", result.get().getCode());
    }

    @Test
    void findByCode_InactiveProduct_ReturnsEmpty() {
        existingProduct.setActive(false);
        when(productRepository.findByCode("TELKOMSEL")).thenReturn(Optional.of(existingProduct));

        assertTrue(productService.findByCode("TELKOMSEL").isEmpty());
    }

    @Test
    void findByCategoryForAdmin_ReturnsList() {
        when(productRepository.findByCategoryIdOrderBySortOrder(categoryId))
                .thenReturn(List.of(existingProduct));

        List<Products> result = productService.findByCategoryForAdmin(categoryId);

        assertEquals(1, result.size());
    }

    @Test
    void findById_Found_ReturnsOptional() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));

        Optional<Products> result = productService.findById(productId);

        assertTrue(result.isPresent());
        assertEquals(productId, result.get().getId());
    }

    @Test
    void findById_NotFound_ReturnsEmpty() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertTrue(productService.findById(productId).isEmpty());
    }

    // === CREATE ===

    @Test
    void create_Success() throws BusinessException {
        // Arrange
        CreateProductRequest req = new CreateProductRequest(
                categoryId, "xl", "XL Axiata", "XL", "Paket XL", null, true, 2);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.findByCode("XL")).thenReturn(Optional.empty());
        when(productRepository.save(any(Products.class))).thenAnswer(inv -> {
            Products p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        // Act
        Products result = productService.create(req);

        // Assert
        assertNotNull(result.getId());
        assertEquals("XL", result.getCode()); // uppercased
        assertEquals("XL Axiata", result.getName());
        assertEquals(category, result.getCategory());
        assertTrue(result.isActive());
        assertFalse(result.isDeleted());
    }

    @Test
    void create_CategoryNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        UUID unknownCatId = UUID.randomUUID();
        CreateProductRequest req = new CreateProductRequest(
                unknownCatId, "XL", "XL", null, null, null, true, 1);
        when(categoryRepository.findById(unknownCatId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> productService.create(req));
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_DuplicateCode_ThrowsBusinessException() {
        // Arrange
        CreateProductRequest req = new CreateProductRequest(
                categoryId, "TELKOMSEL", "Telkomsel Lagi", null, null, null, true, 1);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.findByCode("TELKOMSEL")).thenReturn(Optional.of(existingProduct));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> productService.create(req));
        assertEquals("DUPLICATE_CODE", ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    // === UPDATE ===

    @Test
    void update_Success() throws BusinessException {
        // Arrange
        UpdateProductRequest req = new UpdateProductRequest(
                categoryId, "TSEL", "Tsel Updated", "Telkomsel", "Updated desc", "icon.png", false, 10);
        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsByCodeAndIdNot("TSEL", productId)).thenReturn(false);
        when(productRepository.save(any(Products.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Products result = productService.update(productId, req);

        // Assert
        assertEquals("TSEL", result.getCode());
        assertEquals("Tsel Updated", result.getName());
        assertEquals("Telkomsel", result.getProviderName());
        assertFalse(result.isActive());
        assertEquals(10, result.getSortOrder());
    }

    @Test
    void update_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        UpdateProductRequest req = new UpdateProductRequest(
                categoryId, "X", "X", null, null, null, true, 1);
        when(productRepository.findById(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> productService.update(unknownId, req));
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_CategoryNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        UUID unknownCatId = UUID.randomUUID();
        UpdateProductRequest req = new UpdateProductRequest(
                unknownCatId, "TELKOMSEL", "Telkomsel", null, null, null, true, 1);
        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(unknownCatId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> productService.update(productId, req));
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_DuplicateCode_ThrowsBusinessException() {
        // Arrange — code "XL" already used by another product
        UpdateProductRequest req = new UpdateProductRequest(
                categoryId, "XL", "XL Clash", null, null, null, true, 1);
        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsByCodeAndIdNot("XL", productId)).thenReturn(true);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> productService.update(productId, req));
        assertEquals("DUPLICATE_CODE", ex.getErrorCode());
        verify(productRepository, never()).save(any());
    }

    // === SOFT DELETE + CASCADE ===

    @Test
    void softDelete_Success_CascadesDenoms() {
        // Arrange — product has 2 active denoms
        ProductDenoms denom1 = new ProductDenoms();
        denom1.setId(UUID.randomUUID());
        denom1.setCode("TLKM5");
        denom1.setActive(true);
        denom1.setDeleted(false);

        ProductDenoms denom2 = new ProductDenoms();
        denom2.setId(UUID.randomUUID());
        denom2.setCode("TLKM10");
        denom2.setActive(true);
        denom2.setDeleted(false);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(denomRepository.findByProductIdOrderBySortOrder(productId))
                .thenReturn(List.of(denom1, denom2));
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(Products.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productService.softDelete(productId);

        // Assert — both denoms soft-deleted
        verify(denomRepository, times(2)).save(any(ProductDenoms.class));
        assertTrue(denom1.isDeleted());
        assertFalse(denom1.isActive());
        assertTrue(denom2.isDeleted());
        assertFalse(denom2.isActive());

        // Assert — product itself soft-deleted
        assertTrue(existingProduct.isDeleted());
        assertFalse(existingProduct.isActive());
    }

    @Test
    void softDelete_SkipsAlreadyDeletedDenoms() {
        // Arrange — 1 active denom + 1 already-deleted denom
        ProductDenoms activeDenom = new ProductDenoms();
        activeDenom.setId(UUID.randomUUID());
        activeDenom.setActive(true);
        activeDenom.setDeleted(false);

        ProductDenoms deletedDenom = new ProductDenoms();
        deletedDenom.setId(UUID.randomUUID());
        deletedDenom.setActive(false);
        deletedDenom.setDeleted(true); // already deleted

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(denomRepository.findByProductIdOrderBySortOrder(productId))
                .thenReturn(List.of(activeDenom, deletedDenom));
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(Products.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productService.softDelete(productId);

        // Assert — only active denom saved (already-deleted skipped)
        verify(denomRepository, times(1)).save(any(ProductDenoms.class));
    }

    @Test
    void softDelete_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        when(productRepository.findById(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> productService.softDelete(unknownId));
        verify(productRepository, never()).save(any());
        verify(denomRepository, never()).save(any());
    }
}
