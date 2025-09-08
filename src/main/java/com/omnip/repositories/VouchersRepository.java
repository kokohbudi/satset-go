package com.omnip.repositories;

import com.omnip.entities.Vouchers;
import com.omnip.entities.Operators;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface untuk entity Vouchers.
 * Menyediakan operasi CRUD dasar dan method custom untuk mengakses data voucher.
 * Interface ini secara otomatis diimplementasikan oleh Spring Data JPA.
 */
@Repository
public interface VouchersRepository extends JpaRepository<Vouchers, UUID> {

    /**
     * Mencari voucher berdasarkan kode voucher.
     * 
     * @param code kode voucher yang dicari
     * @return Optional voucher yang ditemukan
     */
    @Cacheable("vouchers")
    Optional<Vouchers> findByCode(String code);

    /**
     * Mencari voucher berdasarkan operator.
     * 
     * @param operator operator yang terkait dengan voucher
     * @return List voucher yang ditemukan
     */
    List<Vouchers> findByOperator(Operators operator);

    /**
     * Mencari voucher berdasarkan operator ID.
     * 
     * @param operatorId ID operator
     * @return List voucher yang ditemukan
     */
    @Query("SELECT v FROM Vouchers v WHERE v.operator.id = :operatorId")
    List<Vouchers> findByOperatorId(@Param("operatorId") UUID operatorId);

    /**
     * Mencari voucher aktif berdasarkan operator.
     * 
     * @param operator operator yang terkait dengan voucher
     * @return List voucher aktif yang ditemukan
     */
    List<Vouchers> findByOperatorAndActiveTrue(Operators operator);

    /**
     * Mencari voucher berdasarkan range denominasi.
     * 
     * @param minDenomination denominasi minimum
     * @param maxDenomination denominasi maksimum
     * @return List voucher yang ditemukan
     */
    List<Vouchers> findByDenominationBetween(BigDecimal minDenomination, BigDecimal maxDenomination);

    /**
     * Mencari voucher aktif yang belum dihapus.
     * 
     * @return List voucher aktif
     */
    List<Vouchers> findByActiveTrueAndDeletedFalse();

    /**
     * Mencari voucher berdasarkan kode dan status aktif.
     * 
     * @param code kode voucher
     * @param active status aktif
     * @return Optional voucher yang ditemukan
     */
    Optional<Vouchers> findByCodeAndActive(String code, boolean active);

    /**
     * Menghitung jumlah voucher berdasarkan operator.
     * 
     * @param operator operator yang terkait
     * @return jumlah voucher
     */
    long countByOperator(Operators operator);
}
