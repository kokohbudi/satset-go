package com.omnip.repositories;

import com.omnip.entities.PurchasePrices;
import com.omnip.entities.Vouchers;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface untuk entity PurchasePrices.
 * Menyediakan operasi CRUD dasar dan method custom untuk mengakses data harga beli.
 * Interface ini secara otomatis diimplementasikan oleh Spring Data JPA.
 */
@Repository
public interface PurchasePricesRepository extends JpaRepository<PurchasePrices, UUID> {

    /**
     * Mencari harga beli berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return List harga beli yang ditemukan
     */
    List<PurchasePrices> findByVouchers(Vouchers vouchers);

    /**
     * Mencari harga beli berdasarkan voucher ID.
     * 
     * @param voucherId ID voucher
     * @return List harga beli yang ditemukan
     */
    @Query("SELECT pp FROM PurchasePrices pp WHERE pp.vouchers.id = :voucherId")
    List<PurchasePrices> findByVoucherId(@Param("voucherId") UUID voucherId);

    /**
     * Mencari harga beli aktif berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return List harga beli aktif yang ditemukan
     */
    List<PurchasePrices> findByVouchersAndActiveTrue(Vouchers vouchers);

    /**
     * Mencari harga beli yang berlaku pada tanggal tertentu.
     * 
     * @param vouchers voucher yang terkait
     * @param date tanggal efektif
     * @return List harga beli yang berlaku
     */
    List<PurchasePrices> findByVouchersAndEffectiveDateLessThanEqual(Vouchers vouchers, Date date);

    /**
     * Mencari harga beli terbaru berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return Optional harga beli terbaru
     */
    @Cacheable("purchasePrices")
    @Query("SELECT pp FROM PurchasePrices pp WHERE pp.vouchers = :vouchers AND pp.active = true AND pp.deleted = false ORDER BY pp.effectiveDate DESC")
    Optional<PurchasePrices> findLatestByVouchers(@Param("vouchers") Vouchers vouchers);

    /**
     * Mencari harga beli berdasarkan range harga.
     * 
     * @param minPrice harga minimum
     * @param maxPrice harga maksimum
     * @return List harga beli dalam range
     */
    List<PurchasePrices> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Mencari harga beli aktif yang belum dihapus.
     * 
     * @return List harga beli aktif
     */
    List<PurchasePrices> findByActiveTrueAndDeletedFalse();

    /**
     * Mencari harga beli berdasarkan tanggal efektif dalam range.
     * 
     * @param startDate tanggal mulai
     * @param endDate tanggal akhir
     * @return List harga beli dalam range tanggal
     */
    List<PurchasePrices> findByEffectiveDateBetween(Date startDate, Date endDate);

    /**
     * Menghitung jumlah harga beli berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return jumlah harga beli
     */
    long countByVouchers(Vouchers vouchers);
}
