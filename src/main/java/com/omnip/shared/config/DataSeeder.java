package com.omnip.shared.config;

import com.omnip.catalog.domain.model.Categories;
import com.omnip.catalog.domain.model.ProductDenomMeta;
import com.omnip.catalog.domain.model.ProductDenoms;
import com.omnip.catalog.domain.model.Products;
import com.omnip.catalog.domain.model.CategoryType;
import com.omnip.catalog.domain.model.DenomType;
import com.omnip.catalog.adapter.out.persistence.CategoryJpaRepository;
import com.omnip.catalog.adapter.out.persistence.DenomMetaJpaRepository;
import com.omnip.catalog.adapter.out.persistence.DenomJpaRepository;
import com.omnip.catalog.adapter.out.persistence.ProductJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Profile("!prod")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CategoryJpaRepository categoryRepository;
    private final ProductJpaRepository productRepository;
    private final DenomJpaRepository denomRepository;
    private final DenomMetaJpaRepository metaRepository;

    public DataSeeder(CategoryJpaRepository categoryRepository,
                      ProductJpaRepository productRepository,
                      DenomJpaRepository denomRepository,
                      DenomMetaJpaRepository metaRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.denomRepository = denomRepository;
        this.metaRepository = metaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            log.info("Data already seeded, skipping...");
            return;
        }

        log.info("Seeding product catalog data...");
        seedPulsa();
        seedPaketData();
        seedGame();
        seedPlnPostpaid();
        seedEwallet();
        log.info("Product catalog data seeded successfully!");
    }

    // ========== PULSA ==========
    private void seedPulsa() {
        Categories pulsa = createCategory("PULSA", "Pulsa", CategoryType.PREPAID, "/icons/pulsa.svg", 1);

        // Telkomsel
        Products telkomsel = createProduct(pulsa, "TELKOMSEL", "Telkomsel", "Telkomsel", "Pulsa Telkomsel", "/icons/telkomsel.svg", 1);
        createDenom(telkomsel, "TSEL5", "Pulsa 5.000", DenomType.FIXED_DENOM, 5000, 5500, 4800, 0, 30, null, 1);
        createDenom(telkomsel, "TSEL10", "Pulsa 10.000", DenomType.FIXED_DENOM, 10000, 10500, 9800, 0, 30, null, 2);
        createDenom(telkomsel, "TSEL25", "Pulsa 25.000", DenomType.FIXED_DENOM, 25000, 25500, 24500, 0, 30, null, 3);
        createDenom(telkomsel, "TSEL50", "Pulsa 50.000", DenomType.FIXED_DENOM, 50000, 50500, 49000, 0, 30, null, 4);
        createDenom(telkomsel, "TSEL100", "Pulsa 100.000", DenomType.FIXED_DENOM, 100000, 100500, 98000, 0, 30, null, 5);

        // XL
        Products xl = createProduct(pulsa, "XL", "XL Axiata", "XL", "Pulsa XL", "/icons/xl.svg", 2);
        createDenom(xl, "XL5", "Pulsa 5.000", DenomType.FIXED_DENOM, 5000, 5500, 4850, 0, 30, null, 1);
        createDenom(xl, "XL10", "Pulsa 10.000", DenomType.FIXED_DENOM, 10000, 10500, 9850, 0, 30, null, 2);
        createDenom(xl, "XL25", "Pulsa 25.000", DenomType.FIXED_DENOM, 25000, 25500, 24600, 0, 30, null, 3);
        createDenom(xl, "XL50", "Pulsa 50.000", DenomType.FIXED_DENOM, 50000, 50500, 49100, 0, 30, null, 4);
        createDenom(xl, "XL100", "Pulsa 100.000", DenomType.FIXED_DENOM, 100000, 100500, 98100, 0, 30, null, 5);

        // Indosat
        Products indosat = createProduct(pulsa, "INDOSAT", "Indosat Ooredoo", "Indosat", "Pulsa Indosat", "/icons/indosat.svg", 3);
        createDenom(indosat, "ISAT5", "Pulsa 5.000", DenomType.FIXED_DENOM, 5000, 5600, 4900, 0, 30, null, 1);
        createDenom(indosat, "ISAT10", "Pulsa 10.000", DenomType.FIXED_DENOM, 10000, 10600, 9900, 0, 30, null, 2);
        createDenom(indosat, "ISAT25", "Pulsa 25.000", DenomType.FIXED_DENOM, 25000, 25600, 24700, 0, 30, null, 3);
        createDenom(indosat, "ISAT50", "Pulsa 50.000", DenomType.FIXED_DENOM, 50000, 50600, 49200, 0, 30, null, 4);
        createDenom(indosat, "ISAT100", "Pulsa 100.000", DenomType.FIXED_DENOM, 100000, 100600, 98200, 0, 30, null, 5);

        // Tri
        Products tri = createProduct(pulsa, "TRI", "Tri (3)", "Tri", "Pulsa Tri", "/icons/tri.svg", 4);
        createDenom(tri, "TRI5", "Pulsa 5.000", DenomType.FIXED_DENOM, 5000, 5400, 4750, 0, 30, null, 1);
        createDenom(tri, "TRI10", "Pulsa 10.000", DenomType.FIXED_DENOM, 10000, 10400, 9750, 0, 30, null, 2);
        createDenom(tri, "TRI25", "Pulsa 25.000", DenomType.FIXED_DENOM, 25000, 25400, 24400, 0, 30, null, 3);
        createDenom(tri, "TRI50", "Pulsa 50.000", DenomType.FIXED_DENOM, 50000, 50400, 48900, 0, 30, null, 4);
    }

    // ========== PAKET DATA ==========
    private void seedPaketData() {
        Categories data = createCategory("DATA", "Paket Data", CategoryType.PREPAID, "/icons/data.svg", 2);

        // Telkomsel Data
        Products tselData = createProduct(data, "TSEL_DATA", "Telkomsel Data", "Telkomsel", "Paket Internet Telkomsel", "/icons/telkomsel.svg", 1);
        ProductDenoms d1 = createDenom(tselData, "TSEL_DATA_1GB", "Paket 1GB 30 Hari", DenomType.FIXED_DENOM, 1000, 15000, 13000, 0, 30, 1024L, 1);
        createMeta(d1, "description", "Kuota utama 1GB berlaku 30 hari");
        createMeta(d1, "bonus", "Bonus 500MB malam (00:00-06:00)");

        ProductDenoms d2 = createDenom(tselData, "TSEL_DATA_3GB", "Paket 3GB 30 Hari", DenomType.FIXED_DENOM, 3000, 30000, 27000, 0, 30, 3072L, 2);
        createMeta(d2, "description", "Kuota utama 3GB berlaku 30 hari");
        createMeta(d2, "bonus", "Bonus 1GB malam (00:00-06:00)");

        ProductDenoms d3 = createDenom(tselData, "TSEL_DATA_5GB", "Paket 5GB 30 Hari", DenomType.FIXED_DENOM, 5000, 50000, 45000, 0, 30, 5120L, 3);
        createMeta(d3, "description", "Kuota utama 5GB berlaku 30 hari");
        createMeta(d3, "bonus", "Bonus 2GB malam (00:00-06:00)");

        ProductDenoms d4 = createDenom(tselData, "TSEL_DATA_10GB", "Paket 10GB 30 Hari", DenomType.FIXED_DENOM, 10000, 85000, 78000, 0, 30, 10240L, 4);
        createMeta(d4, "description", "Kuota utama 10GB berlaku 30 hari");
        createMeta(d4, "bonus", "Bonus 5GB malam + akses YouTube gratis");

        // XL Data
        Products xlData = createProduct(data, "XL_DATA", "XL Data", "XL", "Paket Internet XL", "/icons/xl.svg", 2);
        createDenom(xlData, "XL_DATA_1GB", "Paket 1GB 30 Hari", DenomType.FIXED_DENOM, 1000, 12000, 10000, 0, 30, 1024L, 1);
        createDenom(xlData, "XL_DATA_3GB", "Paket 3GB 30 Hari", DenomType.FIXED_DENOM, 3000, 28000, 25000, 0, 30, 3072L, 2);
        createDenom(xlData, "XL_DATA_5GB", "Paket 5GB 30 Hari", DenomType.FIXED_DENOM, 5000, 45000, 40000, 0, 30, 5120L, 3);
    }

    // ========== GAME ==========
    private void seedGame() {
        Categories game = createCategory("GAME", "Voucher Game", CategoryType.PREPAID, "/icons/game.svg", 3);

        // Mobile Legends
        Products ml = createProduct(game, "MOBILE_LEGEND", "Mobile Legends", "Moonton", "Diamond Mobile Legends", "/icons/ml.svg", 1);
        createDenom(ml, "ML_86", "86 Diamonds", DenomType.FIXED_DENOM, 86, 20000, 18000, 0, null, null, 1);
        createDenom(ml, "ML_172", "172 Diamonds", DenomType.FIXED_DENOM, 172, 38000, 35000, 0, null, null, 2);
        createDenom(ml, "ML_257", "257 Diamonds", DenomType.FIXED_DENOM, 257, 55000, 50000, 0, null, null, 3);
        createDenom(ml, "ML_344", "344 Diamonds", DenomType.FIXED_DENOM, 344, 72000, 66000, 0, null, null, 4);
        createDenom(ml, "ML_514", "514 Diamonds", DenomType.FIXED_DENOM, 514, 105000, 97000, 0, null, null, 5);

        // Free Fire
        Products ff = createProduct(game, "FREE_FIRE", "Free Fire", "Garena", "Diamond Free Fire", "/icons/freefire.svg", 2);
        createDenom(ff, "FF_70", "70 Diamonds", DenomType.FIXED_DENOM, 70, 10000, 9000, 0, null, null, 1);
        createDenom(ff, "FF_140", "140 Diamonds", DenomType.FIXED_DENOM, 140, 20000, 18000, 0, null, null, 2);
        createDenom(ff, "FF_355", "355 Diamonds", DenomType.FIXED_DENOM, 355, 50000, 45000, 0, null, null, 3);
        createDenom(ff, "FF_720", "720 Diamonds", DenomType.FIXED_DENOM, 720, 100000, 90000, 0, null, null, 4);
    }

    // ========== PLN POSTPAID ==========
    private void seedPlnPostpaid() {
        Categories pln = createCategory("PLN_POSTPAID", "PLN Pascabayar", CategoryType.POSTPAID, "/icons/pln.svg", 4);

        Products plnPostpaid = createProduct(pln, "PLN_PASCABAYAR", "PLN Pascabayar", "PLN", "Pembayaran tagihan listrik PLN", "/icons/pln.svg", 1);

        ProductDenoms plnDenom = new ProductDenoms();
        plnDenom.setProduct(plnPostpaid);
        plnDenom.setCode("PLN_POSTPAID_BAYAR");
        plnDenom.setName("Bayar Tagihan PLN");
        plnDenom.setDenomType(DenomType.OPEN_AMOUNT);
        plnDenom.setAdminFee(new BigDecimal("2500"));
        plnDenom.setMinAmount(new BigDecimal("20000"));
        plnDenom.setMaxAmount(new BigDecimal("10000000"));
        plnDenom.setRequiresInquiry(true);
        plnDenom.setActive(true);
        plnDenom.setDeleted(false);
        plnDenom.setSortOrder(1);
        denomRepository.save(plnDenom);

        createMeta(plnDenom, "inquiry_fields", "customer_id");
        createMeta(plnDenom, "inquiry_label", "Masukkan ID Pelanggan PLN");

        // PLN Prepaid (Token Listrik)
        Categories plnPrepaid = createCategory("PLN_PREPAID", "Token Listrik", CategoryType.PREPAID, "/icons/pln-token.svg", 5);

        Products plnToken = createProduct(plnPrepaid, "PLN_TOKEN", "Token Listrik PLN", "PLN", "Pembelian token listrik prabayar", "/icons/pln-token.svg", 1);
        createDenom(plnToken, "PLN_TOKEN_20K", "Token 20.000", DenomType.FIXED_DENOM, 20000, 22500, 20500, 2500, null, null, 1);
        createDenom(plnToken, "PLN_TOKEN_50K", "Token 50.000", DenomType.FIXED_DENOM, 50000, 52500, 50500, 2500, null, null, 2);
        createDenom(plnToken, "PLN_TOKEN_100K", "Token 100.000", DenomType.FIXED_DENOM, 100000, 102500, 100500, 2500, null, null, 3);
        createDenom(plnToken, "PLN_TOKEN_200K", "Token 200.000", DenomType.FIXED_DENOM, 200000, 202500, 200500, 2500, null, null, 4);
        createDenom(plnToken, "PLN_TOKEN_500K", "Token 500.000", DenomType.FIXED_DENOM, 500000, 502500, 500500, 2500, null, null, 5);
    }

    // ========== E-WALLET ==========
    private void seedEwallet() {
        Categories ewallet = createCategory("EWALLET", "E-Wallet", CategoryType.PREPAID, "/icons/ewallet.svg", 6);

        // GoPay
        Products gopay = createProduct(ewallet, "GOPAY", "GoPay", "GoPay", "Top up saldo GoPay", "/icons/gopay.svg", 1);
        createDenom(gopay, "GOPAY_20K", "GoPay 20.000", DenomType.FIXED_DENOM, 20000, 21000, 20200, 1000, null, null, 1);
        createDenom(gopay, "GOPAY_50K", "GoPay 50.000", DenomType.FIXED_DENOM, 50000, 51000, 50200, 1000, null, null, 2);
        createDenom(gopay, "GOPAY_100K", "GoPay 100.000", DenomType.FIXED_DENOM, 100000, 101500, 100200, 1500, null, null, 3);

        // OVO
        Products ovo = createProduct(ewallet, "OVO", "OVO", "OVO", "Top up saldo OVO", "/icons/ovo.svg", 2);
        createDenom(ovo, "OVO_20K", "OVO 20.000", DenomType.FIXED_DENOM, 20000, 21000, 20200, 1000, null, null, 1);
        createDenom(ovo, "OVO_50K", "OVO 50.000", DenomType.FIXED_DENOM, 50000, 51000, 50200, 1000, null, null, 2);
        createDenom(ovo, "OVO_100K", "OVO 100.000", DenomType.FIXED_DENOM, 100000, 101500, 100200, 1500, null, null, 3);

        // DANA
        Products dana = createProduct(ewallet, "DANA", "DANA", "DANA", "Top up saldo DANA", "/icons/dana.svg", 3);
        createDenom(dana, "DANA_20K", "DANA 20.000", DenomType.FIXED_DENOM, 20000, 21000, 20200, 1000, null, null, 1);
        createDenom(dana, "DANA_50K", "DANA 50.000", DenomType.FIXED_DENOM, 50000, 51000, 50200, 1000, null, null, 2);
        createDenom(dana, "DANA_100K", "DANA 100.000", DenomType.FIXED_DENOM, 100000, 101500, 100200, 1500, null, null, 3);
    }

    // ========== HELPER METHODS ==========

    private Categories createCategory(String code, String name, CategoryType type, String iconUrl, int sortOrder) {
        Categories category = new Categories();
        category.setCode(code);
        category.setName(name);
        category.setCategoryType(type);
        category.setIconUrl(iconUrl);
        category.setActive(true);
        category.setDeleted(false);
        category.setSortOrder(sortOrder);
        return categoryRepository.save(category);
    }

    private Products createProduct(Categories category, String code, String name, String providerName,
                                   String description, String iconUrl, int sortOrder) {
        Products product = new Products();
        product.setCategory(category);
        product.setCode(code);
        product.setName(name);
        product.setProviderName(providerName);
        product.setDescription(description);
        product.setIconUrl(iconUrl);
        product.setActive(true);
        product.setDeleted(false);
        product.setSortOrder(sortOrder);
        return productRepository.save(product);
    }

    private ProductDenoms createDenom(Products product, String code, String name, DenomType denomType,
                                      long nominal, long price, long basePrice, long adminFee,
                                      Integer validityDays, Long quotaMb, int sortOrder) {
        ProductDenoms denom = new ProductDenoms();
        denom.setProduct(product);
        denom.setCode(code);
        denom.setName(name);
        denom.setDenomType(denomType);
        denom.setNominal(BigDecimal.valueOf(nominal));
        denom.setPrice(BigDecimal.valueOf(price));
        denom.setBasePrice(BigDecimal.valueOf(basePrice));
        denom.setAdminFee(BigDecimal.valueOf(adminFee));
        denom.setValidityDays(validityDays);
        denom.setQuotaMb(quotaMb);
        denom.setRequiresInquiry(false);
        denom.setActive(true);
        denom.setDeleted(false);
        denom.setSortOrder(sortOrder);
        return denomRepository.save(denom);
    }

    private void createMeta(ProductDenoms denom, String key, String value) {
        ProductDenomMeta meta = new ProductDenomMeta();
        meta.setProductDenom(denom);
        meta.setMetaKey(key);
        meta.setMetaValue(value);
        metaRepository.save(meta);
    }
}
