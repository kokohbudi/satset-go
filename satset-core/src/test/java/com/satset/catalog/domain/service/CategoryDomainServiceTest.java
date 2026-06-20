package com.satset.catalog.domain.service;

import com.satset.catalog.domain.model.Category;
import com.satset.catalog.domain.model.CategoryType;
import com.satset.catalog.domain.port.in.CreateCategoryRequest;
import com.satset.catalog.domain.port.in.UpdateCategoryRequest;
import com.satset.catalog.adapter.out.persistence.CategoryRepository;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryDomainServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryDomainService categoryService;

    private UUID categoryId;
    private Category existingCategory;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        existingCategory = new Category();
        existingCategory.setId(categoryId);
        existingCategory.setCode("PULSA");
        existingCategory.setName("Pulsa");
        existingCategory.setCategoryType(CategoryType.PREPAID);
        existingCategory.setActive(true);
        existingCategory.setDeleted(false);
        existingCategory.setSortOrder(1);
    }

    // === READ / BROWSE ===

    @Test
    void findAll_ReturnsList() {
        when(categoryRepository.findByActiveTrueAndDeletedFalseOrderBySortOrder())
                .thenReturn(List.of(existingCategory));

        List<Category> result = categoryService.findAll();

        assertEquals(1, result.size());
        assertEquals("PULSA", result.get(0).getCode());
    }

    @Test
    void findByCode_ActiveCategory_ReturnsOptional() {
        when(categoryRepository.findByCode("PULSA")).thenReturn(Optional.of(existingCategory));

        Optional<Category> result = categoryService.findByCode("PULSA");

        assertTrue(result.isPresent());
        assertEquals("PULSA", result.get().getCode());
    }

    @Test
    void findByCode_InactiveCategory_ReturnsEmpty() {
        existingCategory.setActive(false);
        when(categoryRepository.findByCode("PULSA")).thenReturn(Optional.of(existingCategory));

        Optional<Category> result = categoryService.findByCode("PULSA");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByCode_NotFound_ReturnsEmpty() {
        when(categoryRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertTrue(categoryService.findByCode("UNKNOWN").isEmpty());
    }

    @Test
    void findByType_ReturnsList() {
        when(categoryRepository.findByCategoryTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(CategoryType.PREPAID))
                .thenReturn(List.of(existingCategory));

        List<Category> result = categoryService.findByType(CategoryType.PREPAID);

        assertEquals(1, result.size());
    }

    @Test
    void findAllForAdmin_ReturnsList() {
        when(categoryRepository.findAllByOrderBySortOrder()).thenReturn(List.of(existingCategory));

        List<Category> result = categoryService.findAllForAdmin();

        assertEquals(1, result.size());
    }

    @Test
    void findById_Found_ReturnsOptional() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));

        Optional<Category> result = categoryService.findById(categoryId);

        assertTrue(result.isPresent());
        assertEquals(categoryId, result.get().getId());
    }

    @Test
    void findById_NotFound_ReturnsEmpty() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertTrue(categoryService.findById(categoryId).isEmpty());
    }

    // === CREATE ===

    @Test
    void create_Success() throws BusinessException {
        // Arrange
        CreateCategoryRequest req = new CreateCategoryRequest(
                "data", "Paket Data", CategoryType.PREPAID, null, true, 2);
        when(categoryRepository.findByCode("DATA")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category cat = inv.getArgument(0);
            cat.setId(UUID.randomUUID());
            return cat;
        });

        // Act
        Category result = categoryService.create(req);

        // Assert
        assertNotNull(result.getId());
        assertEquals("DATA", result.getCode()); // uppercased
        assertEquals("Paket Data", result.getName());
        assertEquals(CategoryType.PREPAID, result.getCategoryType());
        assertTrue(result.isActive());
        assertFalse(result.isDeleted());
        assertEquals(2, result.getSortOrder());
    }

    @Test
    void create_CodeIsUppercasedAndTrimmed() throws BusinessException {
        // Arrange — code with whitespace and lowercase
        CreateCategoryRequest req = new CreateCategoryRequest(
                "  game  ", "Game Voucher", CategoryType.PREPAID, null, true, 3);
        when(categoryRepository.findByCode("GAME")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Category result = categoryService.create(req);

        // Assert
        assertEquals("GAME", result.getCode());
    }

    @Test
    void create_DuplicateCode_ThrowsBusinessException() {
        // Arrange
        CreateCategoryRequest req = new CreateCategoryRequest(
                "PULSA", "Pulsa Lagi", CategoryType.PREPAID, null, true, 1);
        when(categoryRepository.findByCode("PULSA")).thenReturn(Optional.of(existingCategory));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> categoryService.create(req));
        assertEquals("DUPLICATE_CODE", ex.getErrorCode());
        verify(categoryRepository, never()).save(any());
    }

    // === UPDATE ===

    @Test
    void update_Success() throws BusinessException {
        // Arrange
        UpdateCategoryRequest req = new UpdateCategoryRequest(
                "PULSA_V2", "Pulsa Updated", CategoryType.POSTPAID, "icon.png", false, 5);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByCodeAndIdNot("PULSA_V2", categoryId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Category result = categoryService.update(categoryId, req);

        // Assert
        assertEquals("PULSA_V2", result.getCode());
        assertEquals("Pulsa Updated", result.getName());
        assertEquals(CategoryType.POSTPAID, result.getCategoryType());
        assertEquals("icon.png", result.getIconUrl());
        assertFalse(result.isActive());
        assertEquals(5, result.getSortOrder());
    }

    @Test
    void update_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        UpdateCategoryRequest req = new UpdateCategoryRequest(
                "X", "X", CategoryType.PREPAID, null, true, 1);
        when(categoryRepository.findById(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.update(unknownId, req));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_DuplicateCode_ThrowsBusinessException() {
        // Arrange — code "DATA" already used by another category
        UpdateCategoryRequest req = new UpdateCategoryRequest(
                "DATA", "Data Clash", CategoryType.PREPAID, null, true, 1);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByCodeAndIdNot("DATA", categoryId)).thenReturn(true);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> categoryService.update(categoryId, req));
        assertEquals("DUPLICATE_CODE", ex.getErrorCode());
        verify(categoryRepository, never()).save(any());
    }

    // === SOFT DELETE ===

    @Test
    void softDelete_Success() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        categoryService.softDelete(categoryId);

        // Assert
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        Category saved = captor.getValue();
        assertTrue(saved.isDeleted());
        assertFalse(saved.isActive());
    }

    @Test
    void softDelete_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        when(categoryRepository.findById(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.softDelete(unknownId));
        verify(categoryRepository, never()).save(any());
    }
}
