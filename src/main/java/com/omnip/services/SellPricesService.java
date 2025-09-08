package com.omnip.services;

import com.omnip.entities.SellPrices;
import com.omnip.entities.Vouchers;
import com.omnip.repositories.SellPricesRepository;
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
 * Service class untuk menangani business logic terkait SellPrices.
 * Menyediakan operasi CRUD dan business logic untuk entity SellPrices.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SellPricesService {

    private final SellPricesRepository sellPricesRepository;

    /**
     * Menyimpan harga jual baru atau mengupdate harga jual yang sudah ada.
     * 
     * @param sellPrices entity harga jual yang akan disimpan
     * @return harga jual yang telah disimpan
     */
    public SellPrices save(SellPrices sellPrices) {
        log.info("Saving sell price for voucher: {}", sellPrices.getVouchers().getCode());
        return sellPricesRepository.save(sellPrices);
    }

    /**
     * Mencari harga jual berdasarkan ID.
     * 
     * @param id ID harga jual
     * @return Optional harga jual yang ditemukan
     */
    @Transactional(readOnly = true)
    public Optional<SellPrices> findById(UUID id) {
        log.debug("Finding sell price by id: {}", id);
        return sellPricesRepository.findById(id);
    }

    /**
     * Mencari semua harga jual.
     * 
     * @return List semua harga jual
     */
    @Transactional(readOnly = true)
    public List<SellPrices> findAll() {
        log.debug("Finding all sell prices");
        return sellPricesRepository.findAll();
    }

    /**
     * Mencari harga jual berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return List harga jual yang ditemukan
     */
    @Transactional(readOnly = true)
    public List<SellPrices> findByVouchers(Vouchers vouchers) {
        log.debug("Finding sell prices by voucher: {}", vouchers.getCode());
        return sellPricesRepository.findByVouchers(vouchers);
    }

    /**
     * Mencari harga jual berdasarkan voucher ID.
     * 
     * @param voucherId ID voucher
     * @return List harga jual yang ditemukan
     */
    @Transactional(readOnly = true)
    public List<SellPrices> findByVoucherId(UUID voucherId) {
        log.debug("Finding sell prices by voucher id: {}", voucherId);
        return sellPricesRepository.findByVoucherId(voucherId);
    }

    /**
     * Mencari harga jual aktif berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return List harga jual aktif yang ditemukan
     */
    @Transactional(readOnly = true)
    public List<SellPrices> findActiveByVouchers(Vouchers vouchers) {
        log.debug("Finding active sell prices by voucher: {}", vouchers.getCode());
        return sellPricesRepository.findByVouchersAndActiveTrue(vouchers);
    }

    /**
     * Mencari harga jual yang berlaku pada tanggal tertentu.
     * 
     * @param vouchers voucher yang terkait
     * @param date tanggal efektif
     * @return List harga jual yang berlaku
     */
    @Transactional(readOnly = true)
    public List<SellPrices> findByEffectiveDate(Vouchers vouchers, Date date) {
        log.debug("Finding sell prices by effective date: {} for voucher: {}", date, vouchers.getCode());
        return sellPricesRepository.findByVouchersAndEffectiveDateLessThanEqual(vouchers, date);
    }

    /**
     * Mencari harga jual terbaru berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return Optional harga jual terbaru
     */
    @Transactional(readOnly = true)
    public Optional<SellPrices> findLatestByVouchers(Vouchers vouchers) {
        log.debug("Finding latest sell price by voucher: {}", vouchers.getCode());
        return sellPricesRepository.findLatestByVouchers(vouchers);
    }

    /**
     * Mencari harga jual berdasarkan range harga.
     * 
     * @param minPrice harga minimum
     * @param maxPrice harga maksimum
     * @return List harga jual dalam range
     */
    @Transactional(readOnly = true)
    public List<SellPrices> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Finding sell prices by price range: {} - {}", minPrice, maxPrice);
        return sellPricesRepository.findByPriceBetween(minPrice, maxPrice);
    }

    /**
     * Mencari harga jual aktif yang belum dihapus.
     * 
     * @return List harga jual aktif
     */
    @Transactional(readOnly = true)
    public List<SellPrices> findActivePrices() {
        log.debug("Finding active sell prices");
        return sellPricesRepository.findByActiveTrueAndDeletedFalse();
    }

    /**
     * Mencari harga jual berdasarkan tanggal efektif dalam range.
     * 
     * @param startDate tanggal mulai
     * @param endDate tanggal akhir
     * @return List harga jual dalam range tanggal
     */
    @Transactional(readOnly = true)
    public List<SellPrices> findByEffectiveDateRange(Date startDate, Date endDate) {
        log.debug("Finding sell prices by effective date range: {} - {}", startDate, endDate);
        return sellPricesRepository.findByEffectiveDateBetween(startDate, endDate);
    }

    /**
     * Mengaktifkan harga jual.
     * 
     * @param id ID harga jual
     * @return harga jual yang telah diaktifkan
     */
    public Optional<SellPrices> activatePrice(UUID id) {
        log.info("Activating sell price with id: {}", id);
        Optional<SellPrices> priceOpt = sellPricesRepository.findById(id);
        if (priceOpt.isPresent()) {
            SellPrices price = priceOpt.get();
            price.setActive(true);
            return Optional.of(sellPricesRepository.save(price));
        }
        return Optional.empty();
    }

    /**
     * Menonaktifkan harga jual.
     * 
     * @param id ID harga jual
     * @return harga jual yang telah dinonaktifkan
     */
    public Optional<SellPrices> deactivatePrice(UUID id) {
        log.info("Deactivating sell price with id: {}", id);
        Optional<SellPrices> priceOpt = sellPricesRepository.findById(id);
        if (priceOpt.isPresent()) {
            SellPrices price = priceOpt.get();
            price.setActive(false);
            return Optional.of(sellPricesRepository.save(price));
        }
        return Optional.empty();
    }

    /**
     * Soft delete harga jual.
     * 
     * @param id ID harga jual
     * @return harga jual yang telah dihapus
     */
    public Optional<SellPrices> deletePrice(UUID id) {
        log.info("Soft deleting sell price with id: {}", id);
        Optional<SellPrices> priceOpt = sellPricesRepository.findById(id);
        if (priceOpt.isPresent()) {
            SellPrices price = priceOpt.get();
            price.setDeleted(true);
            price.setActive(false);
            return Optional.of(sellPricesRepository.save(price));
        }
        return Optional.empty();
    }

    /**
     * Menghitung jumlah harga jual berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return jumlah harga jual
     */
    @Transactional(readOnly = true)
    public long countByVouchers(Vouchers vouchers) {
        log.debug("Counting sell prices by voucher: {}", vouchers.getCode());
        return sellPricesRepository.countByVouchers(vouchers);
    }
}
