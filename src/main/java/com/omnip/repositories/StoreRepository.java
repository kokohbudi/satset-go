package com.omnip.repositories;

import com.omnip.entities.Store;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface untuk entity Store.
 * Menyediakan operasi CRUD dasar dan method custom untuk mengakses data toko.
 * Interface ini secara otomatis diimplementasikan oleh Spring Data JPA.
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    /**
     * Memeriksa apakah terdapat toko dengan referral ID tertentu.
     *
     * @param referalId ID referral yang akan dicek keberadaannya
     * @return true jika referral ID sudah ada, false jika belum ada
     */
    boolean existsByReferralId(String referalId);

    /**
     * Mencari toko berdasarkan alamat email.
     * Hasil pencarian akan disimpan dalam cache untuk mempercepat akses berikutnya.
     *
     * @param email Alamat email toko yang dicari
     * @return Objek Store jika ditemukan, null jika tidak ditemukan
     */
    @Cacheable(value = "stores", key = "#email", cacheManager = "fastCacheManager")
    Store findByEmail(String email);
}