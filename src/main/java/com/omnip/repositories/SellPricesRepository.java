package com.omnip.repositories;

import com.omnip.entities.SellPrices;
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
 * Repository interface untuk entity SellPrices.
 * Menyediakan operasi CRUD dasar dan method custom untuk mengakses data harga jual.
 * Interface ini secara otomatis diimplementasikan oleh Spring Data JPA.
 */
@Repository
public interface SellPricesRepository extends JpaRepository<SellPrices, UUID> {

    /**
     * Mencari harga jual berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return List harga jual yang ditemukan
     */
    List<SellPrices> findByVouchers(Vouchers vouchers);

    /**
     * Mencari harga jual berdasarkan voucher ID.
     * 
     * @param voucherId ID voucher
     * @return List harga jual yang ditemukan
     */
    @Query("SELECT sp FROM SellPrices sp WHERE sp.vouchers.id = :voucherId")
    List<SellPrices> findByVoucherId(@Param("voucherId") UUID voucherId);

    /**
     * Mencari harga jual aktif berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return List harga jual aktif yang ditemukan
     */
    List<SellPrices> findByVouchersAndActiveTrue(Vouchers vouchers);

    /**
     * Mencari harga jual yang berlaku pada tanggal tertentu.
     * 
     * @param vouchers voucher yang terkait
     * @param date tanggal efektif
     * @return List harga jual yang berlaku
     */
    List<SellPrices> findByVouchersAndEffectiveDateLessThanEqual(Vouchers vouchers, Date date);

    /**
     * Mencari harga jual terbaru berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return Optional harga jual terbaru
     */
    @Cacheable("sellPrices")
    @Query("SELECT sp FROM SellPrices sp WHERE sp.vouchers = :vouchers AND sp.active = true AND sp.deleted = false ORDER BY sp.effectiveDate DESC")
    Optional<SellPrices> findLatestByVouchers(@Param("vouchers") Vouchers vouchers);

    /**
     * Mencari harga jual berdasarkan range harga.
     * 
     * @param minPrice harga minimum
     * @param maxPrice harga maksimum
     * @return List harga jual dalam range
     */
    List<SellPrices> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Mencari harga jual aktif yang belum dihapus.
     * 
     * @return List harga jual aktif
     */
    List<SellPrices> findByActiveTrueAndDeletedFalse();

    /**
     * Mencari harga jual berdasarkan tanggal efektif dalam range.
     * 
     * @param startDate tanggal mulai
     * @param endDate tanggal akhir
     * @return List harga jual dalam range tanggal
     */
    List<SellPrices> findByEffectiveDateBetween(Date startDate, Date endDate);

    /**
     * Menghitung jumlah harga jual berdasarkan voucher.
     * 
     * @param vouchers voucher yang terkait
     * @return jumlah harga jual
     */
    long countByVouchers(Vouchers vouchers);
}
