package com.omnip.services;

import com.omnip.entities.PurchasePrices;
import com.omnip.entities.Vouchers;
import com.omnip.repositories.PurchasePricesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class untuk menangani business logic terkait PurchasePrices.
 * Menyediakan operasi CRUD dan business logic untuk entity PurchasePrices.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PurchasePricesService {

    private final PurchasePricesRepository purchasePricesRepository;

    /**
     * Menyimpan harga beli baru atau mengupdate harga beli yang sudah ada.
     * 
     * @param purchasePrices entity harga beli yang akan disimpan
     * @return harga beli yang telah disimpan
     */
    public PurchasePrices save(PurchasePrices purchasePrices) {
        log.info("Saving purchase price for voucher: {}", purchasePrices.getVouchers().getCode());
        return purchasePricesRepository.save(purchasePrices);
    }

    /**
     * Mencari harga beli berdasarkan ID.
     * 
     * @param id ID harga beli
     * @return Optional harga beli yang ditemukan
     */
    @Transactional(readOnly = true)
    public Optional<PurchasePrices> findById(UUID id) {
        log.debug("Finding purchase price by id: {}", id);
        return purchasePricesRepository.findById(id);
    }

    /**
     * Mencari semua harga beli.
     * 
     * @return List semua harga beli
     */
    @Transactional(readOnly = true)
    public List<PurchasePrices> findAll() {
        log.debug("Finding all purchase prices");
        return purchasePricesRepository.findAll();
    }

    /**
     * Mencari harga beli berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return List harga beli yang ditemukan
     */
    @Transactional(readOnly = true)
    public List<PurchasePrices> findByVouchers(Vouchers vouchers) {
        log.debug("Finding purchase prices by voucher: {}", vouchers.getCode());
        return purchasePricesRepository.findByVouchers(vouchers);
    }

    /**
     * Mencari harga beli berdasarkan voucher ID.
     * 
     * @param voucherId ID voucher
     * @return List harga beli yang ditemukan
     */
    @Transactional(readOnly = true)
    public List<PurchasePrices> findByVoucherId(UUID voucherId) {
        log.debug("Finding purchase prices by voucher id: {}", voucherId);
        return purchasePricesRepository.findByVoucherId(voucherId);
    }

    /**
     * Mencari harga beli aktif berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return List harga beli aktif yang ditemukan
     */
    @Transactional(readOnly = true)
    public List<PurchasePrices> findActiveByVouchers(Vouchers vouchers) {
        log.debug("Finding active purchase prices by voucher: {}", vouchers.getCode());
        return purchasePricesRepository.findByVouchersAndActiveTrue(vouchers);
    }

    /**
     * Mencari harga beli yang berlaku pada tanggal tertentu.
     * 
     * @param vouchers voucher yang terkait
     * @param date tanggal efektif
     * @return List harga beli yang berlaku
     */
    @Transactional(readOnly = true)
    public List<PurchasePrices> findByEffectiveDate(Vouchers vouchers, Date date) {
        log.debug("Finding purchase prices by effective date: {} for voucher: {}", date, vouchers.getCode());
        return purchasePricesRepository.findByVouchersAndEffectiveDateLessThanEqual(vouchers, date);
    }

    /**
     * Mencari harga beli terbaru berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return Optional harga beli terbaru
     */
    @Transactional(readOnly = true)
    public Optional<PurchasePrices> findLatestByVouchers(Vouchers vouchers) {
        log.debug("Finding latest purchase price by voucher: {}", vouchers.getCode());
        return purchasePricesRepository.findLatestByVouchers(vouchers);
    }

    /**
     * Mencari harga beli berdasarkan range harga.
     * 
     * @param minPrice harga minimum
     * @param maxPrice harga maksimum
     * @return List harga beli dalam range
     */
    @Transactional(readOnly = true)
    public List<PurchasePrices> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Finding purchase prices by price range: {} - {}", minPrice, maxPrice);
        return purchasePricesRepository.findByPriceBetween(minPrice, maxPrice);
    }

    /**
     * Mencari harga beli aktif yang belum dihapus.
     * 
     * @return List harga beli aktif
     */
    @Transactional(readOnly = true)
    public List<PurchasePrices> findActivePrices() {
        log.debug("Finding active purchase prices");
        return purchasePricesRepository.findByActiveTrueAndDeletedFalse();
    }

    /**
     * Mencari harga beli berdasarkan tanggal efektif dalam range.
     * 
     * @param startDate tanggal mulai
     * @param endDate tanggal akhir
     * @return List harga beli dalam range tanggal
     */
    @Transactional(readOnly = true)
    public List<PurchasePrices> findByEffectiveDateRange(Date startDate, Date endDate) {
        log.debug("Finding purchase prices by effective date range: {} - {}", startDate, endDate);
        return purchasePricesRepository.findByEffectiveDateBetween(startDate, endDate);
    }

    /**
     * Mengaktifkan harga beli.
     * 
     * @param id ID harga beli
     * @return harga beli yang telah diaktifkan
     */
    public Optional<PurchasePrices> activatePrice(UUID id) {
        log.info("Activating purchase price with id: {}", id);
        Optional<PurchasePrices> priceOpt = purchasePricesRepository.findById(id);
        if (priceOpt.isPresent()) {
            PurchasePrices price = priceOpt.get();
            price.setActive(true);
            return Optional.of(purchasePricesRepository.save(price));
        }
        return Optional.empty();
    }

    /**
     * Menonaktifkan harga beli.
     * 
     * @param id ID harga beli
     * @return harga beli yang telah dinonaktifkan
     */
    public Optional<PurchasePrices> deactivatePrice(UUID id) {
        log.info("Deactivating purchase price with id: {}", id);
        Optional<PurchasePrices> priceOpt = purchasePricesRepository.findById(id);
        if (priceOpt.isPresent()) {
            PurchasePrices price = priceOpt.get();
            price.setActive(false);
            return Optional.of(purchasePricesRepository.save(price));
        }
        return Optional.empty();
    }

    /**
     * Soft delete harga beli.
     * 
     * @param id ID harga beli
     * @return harga beli yang telah dihapus
     */
    public Optional<PurchasePrices> deletePrice(UUID id) {
        log.info("Soft deleting purchase price with id: {}", id);
        Optional<PurchasePrices> priceOpt = purchasePricesRepository.findById(id);
        if (priceOpt.isPresent()) {
            PurchasePrices price = priceOpt.get();
            price.setDeleted(true);
            price.setActive(false);
            return Optional.of(purchasePricesRepository.save(price));
        }
        return Optional.empty();
    }

    /**
     * Menghitung jumlah harga beli berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return jumlah harga beli
     */
    @Transactional(readOnly = true)
    public long countByVouchers(Vouchers vouchers) {
        log.debug("Counting purchase prices by voucher: {}", vouchers.getCode());
        return purchasePricesRepository.countByVouchers(vouchers);
    }
}
