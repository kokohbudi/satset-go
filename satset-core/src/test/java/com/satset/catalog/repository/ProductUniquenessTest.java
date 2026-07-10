package com.satset.catalog.repository;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.CategoryType;
import com.satset.catalog.model.Products;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ProductUniquenessTest {

    @Autowired ProductRepository productRepository;
    @Autowired CategoryRepository categoryRepository;

    private UUID newCategory(String code) {
        Category c = new Category();
        c.setCode(code); c.setName(code); c.setActive(true); c.setDeleted(false);
        c.setCategoryType(CategoryType.PREPAID);
        return categoryRepository.save(c).getId();
    }

    private Products product(UUID categoryId, String code) {
        Products p = new Products();
        p.setCategoryId(categoryId); p.setCode(code); p.setName(code);
        p.setActive(true); p.setDeleted(false);
        return p;
    }

    @Test
    void sameBrandCode_inTwoCategories_bothPersist() {
        UUID pulsa = newCategory("T1PULSA");
        UUID data = newCategory("T1DATA");
        productRepository.saveAndFlush(product(pulsa, "TELKOMSEL"));
        productRepository.saveAndFlush(product(data, "TELKOMSEL"));
        assertThat(productRepository.findByCategoryIdAndCode(pulsa, "TELKOMSEL")).isPresent();
        assertThat(productRepository.findByCategoryIdAndCode(data, "TELKOMSEL")).isPresent();
    }

    @Test
    void sameBrandCode_twiceInOneCategory_violatesUnique() {
        UUID pulsa = newCategory("T1PULSA2");
        productRepository.saveAndFlush(product(pulsa, "XL"));
        assertThatThrownBy(() -> productRepository.saveAndFlush(product(pulsa, "XL")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
