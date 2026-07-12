package com.satset.supplier.service;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;
import com.satset.catalog.service.CategoryDomainService;
import com.satset.catalog.service.DenomDomainService;
import com.satset.catalog.service.ProductDomainService;
import com.satset.supplier.client.DigiflazzClient;
import com.satset.supplier.model.CompareStatus;
import com.satset.supplier.model.PriceCompareRow;
import com.satset.supplier.model.PriceListItem;
import com.satset.supplier.model.PriceListSnapshot;
import com.satset.supplier.model.SyncAction;
import com.satset.supplier.model.SyncPreviewItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogSyncServiceTest {

    @Mock DigiflazzClient digiflazz;
    @Mock CategoryDomainService categoryService;
    @Mock ProductDomainService productService;
    @Mock DenomDomainService denomService;

    private CatalogSyncService service() {
        return new CatalogSyncService(digiflazz, categoryService, productService, denomService);
    }

    private static PriceListItem df(String sku, String name, String brand, long price) {
        return dfCat(sku, name, "Pulsa", brand, price);
    }
    private static PriceListItem dfCat(String sku, String name, String category, String brand, long price) {
        return new PriceListItem(name, category, brand, sku, price, true, "0", "Ki***");
    }
    private static ProductDenoms denom(String code, BigDecimal base) {
        ProductDenoms d = new ProductDenoms(); d.setCode(code); d.setName("n"+code);
        d.setBasePrice(base); d.setActive(true); return d;
    }

    // ---- previewCategories ----
    @Test void previewCategories_addsMissing_deletesNotInDf() {
        Category existing = new Category(); existing.setId(UUID.randomUUID()); existing.setCode("PULSA"); existing.setName("Pulsa");
        Category orphan = new Category(); orphan.setId(UUID.randomUUID()); orphan.setCode("GAME"); orphan.setName("Game Lama");
        when(categoryService.findAllForAdmin()).thenReturn(List.of(existing, orphan));
        when(digiflazz.fetchSnapshot()).thenReturn(new PriceListSnapshot(List.of(
                df("a","A","XL",1),                          // category "Pulsa" -> PULSA (exists)
                dfCat("b","B","E-Money","DANA",2)), LocalDateTime.now()));          // category "E-Money" -> EMONEY (new)
        List<SyncPreviewItem> items = service().previewCategories();
        assertThat(items).anySatisfy(i -> { assertThat(i.action()).isEqualTo(SyncAction.ADD); assertThat(i.key()).isEqualTo("E-Money"); });
        assertThat(items).anySatisfy(i -> { assertThat(i.action()).isEqualTo(SyncAction.DELETE); assertThat(i.key()).isEqualTo(orphan.getId().toString()); });
        assertThat(items).noneMatch(i -> "Pulsa".equals(i.key()));
    }

    @Test void applyCategories_appliesOnlySelected() throws Exception {
        Category orphan = new Category(); orphan.setId(UUID.randomUUID()); orphan.setCode("GAME"); orphan.setName("Game Lama");
        when(categoryService.findAllForAdmin()).thenReturn(List.of(orphan));
        when(digiflazz.fetchSnapshot()).thenReturn(new PriceListSnapshot(List.of(dfCat("b","B","E-Money","DANA",2)), LocalDateTime.now()));
        // pilih hanya ADD "E-Money", TIDAK pilih DELETE orphan
        SyncResult r = service().applyCategories(List.of("E-Money"));
        verify(categoryService).findOrCreateByName("E-Money");
        verify(categoryService, never()).softDelete(any());
        assertThat(r.added()).isEqualTo(1);
        assertThat(r.deleted()).isZero();
    }

    // ---- previewProducts ----
    @Test void previewProducts_addsMissingBrand_deletesOrphan() {
        UUID catId = UUID.randomUUID();
        Category cat = new Category(); cat.setId(catId); cat.setCode("PULSA");
        when(categoryService.findById(catId)).thenReturn(Optional.of(cat));
        Products orphan = new Products(); orphan.setId(UUID.randomUUID()); orphan.setCode("OLDBRAND"); orphan.setName("Old");
        when(productService.findByCategoryForAdmin(catId)).thenReturn(List.of(orphan));
        when(productService.findByCategoryAndCode("PULSA", "XL")).thenReturn(Optional.empty());
        when(digiflazz.fetchSnapshot()).thenReturn(new PriceListSnapshot(List.of(df("a","A","XL",1)), LocalDateTime.now()));   // category "Pulsa" -> PULSA
        List<SyncPreviewItem> items = service().previewProducts(catId);
        assertThat(items).anySatisfy(i -> { assertThat(i.action()).isEqualTo(SyncAction.ADD); assertThat(i.key()).isEqualTo("XL"); });
        assertThat(items).anySatisfy(i -> { assertThat(i.action()).isEqualTo(SyncAction.DELETE); assertThat(i.key()).isEqualTo(orphan.getId().toString()); });
    }

    @Test void applyProducts_appliesOnlySelected() {
        UUID catId = UUID.randomUUID();
        Category cat = new Category(); cat.setId(catId); cat.setCode("PULSA");
        when(categoryService.findById(catId)).thenReturn(Optional.of(cat));
        when(productService.findByCategoryForAdmin(catId)).thenReturn(List.of());
        when(productService.findByCategoryAndCode("PULSA", "XL")).thenReturn(Optional.empty());
        when(digiflazz.fetchSnapshot()).thenReturn(new PriceListSnapshot(List.of(df("a","A","XL",1)), LocalDateTime.now()));
        SyncResult r = service().applyProducts(catId, List.of("XL"));
        verify(productService).findOrCreateByBrand("XL", catId);
        assertThat(r.added()).isEqualTo(1);
    }

    // ---- reconcileForProduct ----
    @Test void reconcileForProduct_filtersByBrandCode_computesStatus() {
        UUID pid = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        Products p = new Products(); p.setId(pid); p.setCode("XL"); p.setCategoryId(catId);
        Category pulsa = new Category(); pulsa.setId(catId); pulsa.setCode("PULSA");
        when(productService.findById(pid)).thenReturn(Optional.of(p));
        when(categoryService.findById(catId)).thenReturn(Optional.of(pulsa));
        when(digiflazz.fetchSnapshot()).thenReturn(new PriceListSnapshot(List.of(
                df("x100","XL 100","XL",98000),   // matched->NAIK
                df("x5","XL 5","XL",5500),         // BARU
                dfCat("dana20","D","E-Money","DANA",20000)), LocalDateTime.now())); // beda brand, di-skip
        ProductDenoms d1 = denom("X100", new BigDecimal("97000")); d1.setId(UUID.randomUUID());
        when(denomService.findActiveByProductId(pid)).thenReturn(List.of(
                d1,        // matched
                denom("XOLD", new BigDecimal("1000")))); // HILANG
        List<PriceCompareRow> rows = service().reconcileForProduct(pid);
        assertThat(rows).extracting(PriceCompareRow::status)
                .containsExactlyInAnyOrder(CompareStatus.NAIK, CompareStatus.BARU, CompareStatus.HILANG);
        assertThat(rows).noneMatch(r -> "dana20".equals(r.buyerSku()));
    }

    @Test void reconcileForProduct_noDfBrandMatch_returnsEmpty() {
        UUID pid = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        Products p = new Products(); p.setId(pid); p.setCode("XL"); p.setCategoryId(catId);
        Category pulsa = new Category(); pulsa.setId(catId); pulsa.setCode("PULSA");
        when(productService.findById(pid)).thenReturn(Optional.of(p));
        when(categoryService.findById(catId)).thenReturn(Optional.of(pulsa));
        when(digiflazz.fetchSnapshot()).thenReturn(new PriceListSnapshot(List.of(
                dfCat("dana20","D","E-Money","DANA",20000)), LocalDateTime.now())); // beda brand, no match utk XL
        // lenient: guard fix short-circuits before this is ever consulted
        lenient().when(denomService.findActiveByProductId(pid)).thenReturn(List.of(
                denom("X100", new BigDecimal("97000")),
                denom("X5", new BigDecimal("5500"))));
        assertThat(service().reconcileForProduct(pid)).isEmpty();
    }

    @Test void reconcileForProduct_filtersByCategory_excludesOtherCategorySkus() {
        UUID pid = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        Products p = new Products(); p.setId(pid); p.setCode("TELKOMSEL"); p.setCategoryId(catId);
        Category pulsa = new Category(); pulsa.setId(catId); pulsa.setCode("PULSA");
        when(productService.findById(pid)).thenReturn(Optional.of(p));
        when(categoryService.findById(catId)).thenReturn(Optional.of(pulsa));
        when(digiflazz.fetchSnapshot()).thenReturn(new PriceListSnapshot(List.of(
                df("tsel5", "Tsel 5K", "TELKOMSEL", 5500),                       // category "Pulsa" -> PULSA (keep)
                dfCat("tselv10", "Tsel Voucher 10", "Voucher", "TELKOMSEL", 10000)), LocalDateTime.now())); // category "Voucher" (drop)
        when(denomService.findActiveByProductId(pid)).thenReturn(List.of());
        List<PriceCompareRow> rows = service().reconcileForProduct(pid);
        assertThat(rows).extracting(PriceCompareRow::buyerSku).containsExactly("tsel5");
    }

    // ---- applyDenoms (delete = softDelete) ----
    @Test void applyDenoms_appliesSelected_hilangUsesSoftDelete() {
        UUID pid = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        Products p = new Products(); p.setId(pid); p.setCode("XL"); p.setCategoryId(catId);
        Category pulsa = new Category(); pulsa.setId(catId); pulsa.setCode("PULSA");
        when(productService.findById(pid)).thenReturn(Optional.of(p));
        when(categoryService.findById(catId)).thenReturn(Optional.of(pulsa));
        when(digiflazz.fetchSnapshot()).thenReturn(new PriceListSnapshot(List.of(
                df("x5","XL 5","XL",5500)), LocalDateTime.now()));                  // BARU (sku x5)
        UUID dOld = UUID.randomUUID();
        ProductDenoms old = denom("XOLD", new BigDecimal("1000")); old.setId(dOld);
        when(denomService.findActiveByProductId(pid)).thenReturn(List.of(old));  // XOLD -> HILANG
        // pilih BARU x5 dan HILANG XOLD (key HILANG = denom.code "XOLD")
        SyncResult r = service().applyDenoms(pid, List.of("x5", "XOLD"));
        verify(denomService).createFromSupplier(pid, "x5", "XL 5", new BigDecimal("5500"));
        verify(denomService).softDelete(dOld);
        assertThat(r.added()).isEqualTo(1);
        assertThat(r.deleted()).isEqualTo(1);
    }

    @Test void applyDenoms_unselected_skipped() {
        UUID pid = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        Products p = new Products(); p.setId(pid); p.setCode("XL"); p.setCategoryId(catId);
        Category pulsa = new Category(); pulsa.setId(catId); pulsa.setCode("PULSA");
        when(productService.findById(pid)).thenReturn(Optional.of(p));
        when(categoryService.findById(catId)).thenReturn(Optional.of(pulsa));
        when(digiflazz.fetchSnapshot()).thenReturn(new PriceListSnapshot(List.of(df("x5","XL 5","XL",5500)), LocalDateTime.now()));
        when(denomService.findActiveByProductId(pid)).thenReturn(List.of());
        SyncResult r = service().applyDenoms(pid, List.of());  // pilih kosong
        verify(denomService, never()).createFromSupplier(any(), any(), any(), any());
        assertThat(r.added()).isZero();
    }

    // ---- syncAll: tambah + update + set flag, TANPA delete ----
    @Test void syncAll_addsDenom_setsFlags_neverDeletes() throws Exception {
        UUID catId = UUID.randomUUID(), pid = UUID.randomUUID();
        Category pulsa = new Category(); pulsa.setId(catId); pulsa.setCode("PULSA"); pulsa.setName("Pulsa");
        Products xl = new Products(); xl.setId(pid); xl.setCode("XL"); xl.setName("XL"); xl.setCategoryId(catId);

        when(digiflazz.fetchSnapshot()).thenReturn(new PriceListSnapshot(List.of(df("xl5","XL 5K","XL",5000)), LocalDateTime.now()));
        when(categoryService.findAllForAdmin()).thenReturn(List.of(pulsa));
        when(categoryService.findById(catId)).thenReturn(Optional.of(pulsa));
        when(productService.findByCategoryForAdmin(catId)).thenReturn(List.of(xl));
        when(productService.findByCategoryAndCode("PULSA", "XL")).thenReturn(Optional.of(xl));
        when(productService.findById(pid)).thenReturn(Optional.of(xl));
        when(denomService.findActiveByProductId(pid)).thenReturn(List.of());  // denom baru → BARU

        SyncResult r = service().syncAll();

        verify(denomService).createFromSupplier(eq(pid), eq("xl5"), any(), any());
        assertThat(r.added()).isEqualTo(1);
        assertThat(r.deleted()).isZero();
        verify(categoryService).reconcileSupplierFlags(argThat(s -> s.contains("PULSA")));
        verify(productService).reconcileSupplierFlags(eq(catId), argThat(s -> s.contains("XL")));
        verify(denomService).reconcileSupplierFlags(eq(pid), argThat(s -> s.contains("XL5")));
        verify(categoryService, never()).softDelete(any());
        verify(productService, never()).softDelete(any());
        verify(denomService, never()).softDelete(any());
    }

    // ---- syncAllPreview: read-only aggregate summary of what syncAll() would change ----
    @Test void syncAllPreview_listsNewCategories() {
        // DF has a category the catalog lacks -> previewCategories() yields an ADD
        when(digiflazz.fetchSnapshot()).thenReturn(new PriceListSnapshot(List.of(
                new PriceListItem("Tsel 5rb", "Pulsa", "Telkomsel", "tsel5", 5000L, true, "ok", "S")), LocalDateTime.now()));
        when(categoryService.findAllForAdmin()).thenReturn(List.of()); // nothing yet

        SyncAllPreview p = service().syncAllPreview();

        assertThat(p.newCategories()).contains("Pulsa");
    }

    @Test void syncAllPreview_listsNewProductsNewDenomsAndPriceChanges() {
        UUID catId = UUID.randomUUID();
        UUID pid = UUID.randomUUID();
        Category pulsa = new Category(); pulsa.setId(catId); pulsa.setCode("PULSA"); pulsa.setName("Pulsa");
        Products tsel = new Products(); tsel.setId(pid); tsel.setCode("TELKOMSEL"); tsel.setName("Telkomsel"); tsel.setCategoryId(catId);

        when(digiflazz.fetchSnapshot()).thenReturn(new PriceListSnapshot(List.of(
                df("xl1", "XL 1K", "XL", 1000),              // new product: brand XL not yet in catalog
                df("tsel5", "Tsel 5K", "TELKOMSEL", 5000),   // new denom (BARU): sku not in db
                df("tsel10", "Tsel 10K", "TELKOMSEL", 10500) // price change (NAIK): db has 10000
        ), LocalDateTime.now()));
        when(categoryService.findAllForAdmin()).thenReturn(List.of(pulsa));
        when(categoryService.findById(catId)).thenReturn(Optional.of(pulsa));
        when(productService.findByCategoryForAdmin(catId)).thenReturn(List.of(tsel));
        when(productService.findByCategoryAndCode("PULSA", "XL")).thenReturn(Optional.empty());
        when(productService.findByCategoryAndCode("PULSA", "TELKOMSEL")).thenReturn(Optional.of(tsel));
        when(productService.findById(pid)).thenReturn(Optional.of(tsel));
        when(denomService.findActiveByProductId(pid)).thenReturn(List.of(denom("TSEL10", new BigDecimal("10000"))));

        SyncAllPreview p = service().syncAllPreview();

        assertThat(p.newProducts()).contains("Pulsa / XL");
        assertThat(p.newDenoms()).contains("Telkomsel / Tsel 5K");
        assertThat(p.priceChanges()).contains("Telkomsel / tsel10");
    }

    // ---- supplierPrices: global SKU->cheapest cost map ----
    @Test void supplierPrices_mapsSkuUpperToCost_lowestWins() {
        LocalDateTime at = LocalDateTime.of(2026, 7, 12, 8, 0);
        when(digiflazz.fetchSnapshot()).thenReturn(new PriceListSnapshot(List.of(
                df("tsel5", "Tsel 5rb", "Telkomsel", 5200L),
                df("tsel5", "Tsel 5rb", "Telkomsel", 5000L)
        ), at));

        SupplierPriceView v = service().supplierPrices();

        assertThat(v.fetchedAt()).isEqualTo(at);
        assertThat(v.prices()).containsEntry("TSEL5", 5000L); // lowest price wins
    }
}
