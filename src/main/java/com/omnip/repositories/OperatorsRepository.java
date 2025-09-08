package com.omnip.repositories;

import com.omnip.entities.Operators;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface untuk entity Operators.
 * Menyediakan operasi CRUD dasar dan method custom untuk mengakses data operator.
 * Interface ini secara otomatis diimplementasikan oleh Spring Data JPA.
 */
@Repository
public interface OperatorsRepository extends JpaRepository<Operators, UUID> {

    /**
     * Mencari operator berdasarkan kode operator.
     * 
     * @param code kode operator yang dicari
     * @return Optional operator yang ditemukan
     */
    @Cacheable("operators")
    Optional<Operators> findByCode(String code);

    /**
     * Mencari operator berdasarkan nama.
     * 
     * @param name nama operator yang dicari
     * @return Optional operator yang ditemukan
     */
    Optional<Operators> findByName(String name);

    /**
     * Mencari operator berdasarkan nama yang mengandung kata kunci.
     * 
     * @param keyword kata kunci untuk pencarian
     * @return List operator yang ditemukan
     */
    List<Operators> findByNameContainingIgnoreCase(String keyword);

    /**
     * Mencari operator berdasarkan kode yang mengandung kata kunci.
     * 
     * @param keyword kata kunci untuk pencarian
     * @return List operator yang ditemukan
     */
    List<Operators> findByCodeContainingIgnoreCase(String keyword);

    /**
     * Mencari operator aktif.
     * 
     * @return List operator aktif
     */
    List<Operators> findByActiveTrue();

    /**
     * Mencari operator berdasarkan kode dan status aktif.
     * 
     * @param code kode operator
     * @param active status aktif
     * @return Optional operator yang ditemukan
     */
    Optional<Operators> findByCodeAndActive(String code, boolean active);

    /**
     * Mencari operator berdasarkan nama dan status aktif.
     * 
     * @param name nama operator
     * @param active status aktif
     * @return Optional operator yang ditemukan
     */
    Optional<Operators> findByNameAndActive(String name, boolean active);

    /**
     * Memeriksa apakah operator dengan kode tertentu sudah ada.
     * 
     * @param code kode operator
     * @return true jika sudah ada, false jika belum
     */
    boolean existsByCode(String code);

    /**
     * Memeriksa apakah operator dengan nama tertentu sudah ada.
     * 
     * @param name nama operator
     * @return true jika sudah ada, false jika belum
     */
    boolean existsByName(String name);

    /**
     * Menghitung jumlah operator aktif.
     * 
     * @return jumlah operator aktif
     */
    @Query("SELECT COUNT(o) FROM Operators o WHERE o.active = true")
    long countActiveOperators();
}
