package com.omnip.controllers;

import com.omnip.entities.SellPrices;
import com.omnip.entities.Vouchers;
import com.omnip.services.SellPricesService;
import com.omnip.services.VouchersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller untuk menangani operasi-operasi terkait SellPrices.
 * Menyediakan REST API endpoints untuk manajemen harga jual.
 */
@Slf4j
@RestController
@RequestMapping("/api/sell-prices")
@RequiredArgsConstructor
public class SellPricesController {

    private final SellPricesService sellPricesService;
    private final VouchersService vouchersService;

    /**
     * Mendapatkan semua harga jual.
     * 
     * @return ResponseEntity dengan list harga jual
     */
    @GetMapping
    public ResponseEntity<List<SellPrices>> getAllSellPrices() {
        log.info("Getting all sell prices");
        List<SellPrices> sellPrices = sellPricesService.findAll();
        return ResponseEntity.ok(sellPrices);
    }

    /**
     * Mendapatkan harga jual berdasarkan ID.
     * 
     * @param id ID harga jual
     * @return ResponseEntity dengan harga jual yang ditemukan
     */
    @GetMapping("/{id}")
    public ResponseEntity<SellPrices> getSellPriceById(@PathVariable UUID id) {
        log.info("Getting sell price by id: {}", id);
        Optional<SellPrices> sellPrice = sellPricesService.findById(id);
        return sellPrice.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mendapatkan harga jual berdasarkan voucher ID.
     * 
     * @param voucherId ID voucher
     * @return ResponseEntity dengan list harga jual
     */
    @GetMapping("/voucher/{voucherId}")
    public ResponseEntity<List<SellPrices>> getSellPricesByVoucherId(@PathVariable UUID voucherId) {
        log.info("Getting sell prices by voucher id: {}", voucherId);
        List<SellPrices> sellPrices = sellPricesService.findByVoucherId(voucherId);
        return ResponseEntity.ok(sellPrices);
    }

    /**
     * Mendapatkan harga jual aktif berdasarkan voucher ID.
     * 
     * @param voucherId ID voucher
     * @return ResponseEntity dengan list harga jual aktif
     */
    @GetMapping("/voucher/{voucherId}/active")
    public ResponseEntity<List<SellPrices>> getActiveSellPricesByVoucherId(@PathVariable UUID voucherId) {
        log.info("Getting active sell prices by voucher id: {}", voucherId);
        Optional<Vouchers> voucher = vouchersService.findById(voucherId);
        if (voucher.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<SellPrices> sellPrices = sellPricesService.findActiveByVouchers(voucher.get());
        return ResponseEntity.ok(sellPrices);
    }

    /**
     * Mendapatkan harga jual terbaru berdasarkan voucher ID.
     * 
     * @param voucherId ID voucher
     * @return ResponseEntity dengan harga jual terbaru
     */
    @GetMapping("/voucher/{voucherId}/latest")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    public ResponseEntity<SellPrices> getLatestSellPriceByVoucherId(@PathVariable UUID voucherId) {
        log.info("Getting latest sell price by voucher id: {}", voucherId);
        Optional<Vouchers> voucher = vouchersService.findById(voucherId);
        if (voucher.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Optional<SellPrices> sellPrice = sellPricesService.findLatestByVouchers(voucher.get());
        return sellPrice.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mendapatkan harga jual aktif.
     * 
     * @return ResponseEntity dengan list harga jual aktif
     */
    @GetMapping("/active")
    public ResponseEntity<List<SellPrices>> getActiveSellPrices() {
        log.info("Getting active sell prices");
        List<SellPrices> sellPrices = sellPricesService.findActivePrices();
        return ResponseEntity.ok(sellPrices);
    }

    /**
     * Mendapatkan harga jual berdasarkan range harga.
     * 
     * @param min harga minimum
     * @param max harga maksimum
     * @return ResponseEntity dengan list harga jual
     */
    @GetMapping("/price-range")
    public ResponseEntity<List<SellPrices>> getSellPricesByPriceRange(
            @RequestParam BigDecimal min, 
            @RequestParam BigDecimal max) {
        log.info("Getting sell prices by price range: {} - {}", min, max);
        List<SellPrices> sellPrices = sellPricesService.findByPriceRange(min, max);
        return ResponseEntity.ok(sellPrices);
    }

    /**
     * Mendapatkan harga jual berdasarkan range tanggal efektif.
     * 
     * @param startDate tanggal mulai
     * @param endDate tanggal akhir
     * @return ResponseEntity dengan list harga jual
     */
    @GetMapping("/effective-date-range")
    public ResponseEntity<List<SellPrices>> getSellPricesByEffectiveDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        log.info("Getting sell prices by effective date range: {} - {}", startDate, endDate);
        List<SellPrices> sellPrices = sellPricesService.findByEffectiveDateRange(startDate, endDate);
        return ResponseEntity.ok(sellPrices);
    }

    /**
     * Membuat harga jual baru.
     * 
     * @param sellPrices entity harga jual yang akan dibuat
     * @return ResponseEntity dengan harga jual yang telah dibuat
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<SellPrices> createSellPrice(@RequestBody SellPrices sellPrices) {
        log.info("Creating new sell price for voucher: {}", 
                sellPrices.getVouchers() != null ? sellPrices.getVouchers().getId() : "null");
        try {
            // Validate voucher exists
            if (sellPrices.getVouchers() == null || sellPrices.getVouchers().getId() == null) {
                return ResponseEntity.badRequest().build();
            }
            
            Optional<Vouchers> voucher = vouchersService.findById(sellPrices.getVouchers().getId());
            if (voucher.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            sellPrices.setVouchers(voucher.get());
            
            SellPrices savedSellPrice = sellPricesService.save(sellPrices);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedSellPrice);
        } catch (Exception e) {
            log.error("Error creating sell price: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mengupdate harga jual yang sudah ada.
     * 
     * @param id ID harga jual yang akan diupdate
     * @param sellPrices entity harga jual dengan data baru
     * @return ResponseEntity dengan harga jual yang telah diupdate
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<SellPrices> updateSellPrice(@PathVariable UUID id, @RequestBody SellPrices sellPrices) {
        log.info("Updating sell price with id: {}", id);
        try {
            Optional<SellPrices> existingSellPrice = sellPricesService.findById(id);
            if (existingSellPrice.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            sellPrices.setId(id);
            
            // Validate voucher exists if provided
            if (sellPrices.getVouchers() != null && sellPrices.getVouchers().getId() != null) {
                Optional<Vouchers> voucher = vouchersService.findById(sellPrices.getVouchers().getId());
                if (voucher.isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                sellPrices.setVouchers(voucher.get());
            }
            
            SellPrices updatedSellPrice = sellPricesService.save(sellPrices);
            return ResponseEntity.ok(updatedSellPrice);
        } catch (Exception e) {
            log.error("Error updating sell price: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mengaktifkan harga jual.
     * 
     * @param id ID harga jual
     * @return ResponseEntity dengan harga jual yang telah diaktifkan
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<SellPrices> activateSellPrice(@PathVariable UUID id) {
        log.info("Activating sell price with id: {}", id);
        Optional<SellPrices> sellPrice = sellPricesService.activatePrice(id);
        return sellPrice.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Menonaktifkan harga jual.
     * 
     * @param id ID harga jual
     * @return ResponseEntity dengan harga jual yang telah dinonaktifkan
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<SellPrices> deactivateSellPrice(@PathVariable UUID id) {
        log.info("Deactivating sell price with id: {}", id);
        Optional<SellPrices> sellPrice = sellPricesService.deactivatePrice(id);
        return sellPrice.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Soft delete harga jual.
     * 
     * @param id ID harga jual
     * @return ResponseEntity dengan status operasi
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<SellPrices> deleteSellPrice(@PathVariable UUID id) {
        log.info("Deleting sell price with id: {}", id);
        Optional<SellPrices> sellPrice = sellPricesService.deletePrice(id);
        return sellPrice.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Menghitung jumlah harga jual berdasarkan voucher.
     * 
     * @param voucherId ID voucher
     * @return ResponseEntity dengan jumlah harga jual
     */
    @GetMapping("/count/voucher/{voucherId}")
    public ResponseEntity<Long> countSellPricesByVoucher(@PathVariable UUID voucherId) {
        log.info("Counting sell prices by voucher id: {}", voucherId);
        Optional<Vouchers> voucher = vouchersService.findById(voucherId);
        if (voucher.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        long count = sellPricesService.countByVouchers(voucher.get());
        return ResponseEntity.ok(count);
    }
}
