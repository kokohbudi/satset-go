package com.omnip.controllers;

import com.omnip.entities.PurchasePrices;
import com.omnip.entities.Vouchers;
import com.omnip.services.PurchasePricesService;
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
 * Controller untuk menangani operasi-operasi terkait PurchasePrices.
 * Menyediakan REST API endpoints untuk manajemen harga beli.
 */
@Slf4j
@RestController
@RequestMapping("/api/purchase-prices")
@RequiredArgsConstructor
public class PurchasePricesController {

    private final PurchasePricesService purchasePricesService;
    private final VouchersService vouchersService;

    /**
     * Mendapatkan semua harga beli.
     *
     * @return ResponseEntity dengan list harga beli
     */
    @GetMapping
    public ResponseEntity<List<PurchasePrices>> getAllPurchasePrices() {
        log.info("Getting all purchase prices");
        List<PurchasePrices> purchasePrices = this.purchasePricesService.findAll();
        return ResponseEntity.ok(purchasePrices);
    }

    /**
     * Mendapatkan harga beli berdasarkan ID.
     *
     * @param id ID harga beli
     * @return ResponseEntity dengan harga beli yang ditemukan
     */
    @GetMapping("/{id}")
    public ResponseEntity<PurchasePrices> getPurchasePriceById(@PathVariable UUID id) {
        log.info("Getting purchase price by id: {}", id);
        Optional<PurchasePrices> purchasePrice = this.purchasePricesService.findById(id);
        return purchasePrice.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mendapatkan harga beli berdasarkan voucher ID.
     *
     * @param voucherId ID voucher
     * @return ResponseEntity dengan list harga beli
     */
    @GetMapping("/voucher/{voucherId}")
    public ResponseEntity<List<PurchasePrices>> getPurchasePricesByVoucherId(@PathVariable UUID voucherId) {
        log.info("Getting purchase prices by voucher id: {}", voucherId);
        List<PurchasePrices> purchasePrices = this.purchasePricesService.findByVoucherId(voucherId);
        return ResponseEntity.ok(purchasePrices);
    }

    /**
     * Mendapatkan harga beli aktif berdasarkan voucher ID.
     *
     * @param voucherId ID voucher
     * @return ResponseEntity dengan list harga beli aktif
     */
    @GetMapping("/voucher/{voucherId}/active")
    public ResponseEntity<List<PurchasePrices>> getActivePurchasePricesByVoucherId(@PathVariable UUID voucherId) {
        log.info("Getting active purchase prices by voucher id: {}", voucherId);
        Optional<Vouchers> voucher = this.vouchersService.findById(voucherId);
        if (voucher.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<PurchasePrices> purchasePrices = this.purchasePricesService.findActiveByVouchers(voucher.get());
        return ResponseEntity.ok(purchasePrices);
    }

    /**
     * Mendapatkan harga beli terbaru berdasarkan voucher ID.
     *
     * @param voucherId ID voucher
     * @return ResponseEntity dengan harga beli terbaru
     */
    @GetMapping("/voucher/{voucherId}/latest")
    public ResponseEntity<PurchasePrices> getLatestPurchasePriceByVoucherId(@PathVariable UUID voucherId) {
        log.info("Getting latest purchase price by voucher id: {}", voucherId);
        Optional<Vouchers> voucher = this.vouchersService.findById(voucherId);
        if (voucher.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<PurchasePrices> purchasePrice = this.purchasePricesService.findLatestByVouchers(voucher.get());
        return purchasePrice.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mendapatkan harga beli aktif.
     *
     * @return ResponseEntity dengan list harga beli aktif
     */
    @GetMapping("/active")
    public ResponseEntity<List<PurchasePrices>> getActivePurchasePrices() {
        log.info("Getting active purchase prices");
        List<PurchasePrices> purchasePrices = this.purchasePricesService.findActivePrices();
        return ResponseEntity.ok(purchasePrices);
    }

    /**
     * Mendapatkan harga beli berdasarkan range harga.
     *
     * @param min harga minimum
     * @param max harga maksimum
     * @return ResponseEntity dengan list harga beli
     */
    @GetMapping("/price-range")
    public ResponseEntity<List<PurchasePrices>> getPurchasePricesByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        log.info("Getting purchase prices by price range: {} - {}", min, max);
        List<PurchasePrices> purchasePrices = this.purchasePricesService.findByPriceRange(min, max);
        return ResponseEntity.ok(purchasePrices);
    }

    /**
     * Mendapatkan harga beli berdasarkan range tanggal efektif.
     *
     * @param startDate tanggal mulai
     * @param endDate   tanggal akhir
     * @return ResponseEntity dengan list harga beli
     */
    @GetMapping("/effective-date-range")
    public ResponseEntity<List<PurchasePrices>> getPurchasePricesByEffectiveDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        log.info("Getting purchase prices by effective date range: {} - {}", startDate, endDate);
        List<PurchasePrices> purchasePrices = this.purchasePricesService.findByEffectiveDateRange(startDate, endDate);
        return ResponseEntity.ok(purchasePrices);
    }

    /**
     * Membuat harga beli baru.
     *
     * @param purchasePrices entity harga beli yang akan dibuat
     * @return ResponseEntity dengan harga beli yang telah dibuat
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<PurchasePrices> createPurchasePrice(@RequestBody PurchasePrices purchasePrices) {
        log.info("Creating new purchase price for voucher: {}",
                purchasePrices.getVouchers() != null ? purchasePrices.getVouchers().getId() : "null");
        try {
            // Validate voucher exists
            if (purchasePrices.getVouchers() == null || purchasePrices.getVouchers().getId() == null) {
                return ResponseEntity.badRequest().build();
            }

            Optional<Vouchers> voucher = this.vouchersService.findById(purchasePrices.getVouchers().getId());
            if (voucher.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            purchasePrices.setVouchers(voucher.get());

            PurchasePrices savedPurchasePrice = this.purchasePricesService.save(purchasePrices);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPurchasePrice);
        } catch (Exception e) {
            log.error("Error creating purchase price: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mengupdate harga beli yang sudah ada.
     *
     * @param id             ID harga beli yang akan diupdate
     * @param purchasePrices entity harga beli dengan data baru
     * @return ResponseEntity dengan harga beli yang telah diupdate
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<PurchasePrices> updatePurchasePrice(@PathVariable UUID id, @RequestBody PurchasePrices purchasePrices) {
        log.info("Updating purchase price with id: {}", id);
        try {
            Optional<PurchasePrices> existingPurchasePrice = this.purchasePricesService.findById(id);
            if (existingPurchasePrice.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            purchasePrices.setId(id);

            // Validate voucher exists if provided
            if (purchasePrices.getVouchers() != null && purchasePrices.getVouchers().getId() != null) {
                Optional<Vouchers> voucher = this.vouchersService.findById(purchasePrices.getVouchers().getId());
                if (voucher.isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                purchasePrices.setVouchers(voucher.get());
            }

            PurchasePrices updatedPurchasePrice = this.purchasePricesService.save(purchasePrices);
            return ResponseEntity.ok(updatedPurchasePrice);
        } catch (Exception e) {
            log.error("Error updating purchase price: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mengaktifkan harga beli.
     *
     * @param id ID harga beli
     * @return ResponseEntity dengan harga beli yang telah diaktifkan
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<PurchasePrices> activatePurchasePrice(@PathVariable UUID id) {
        log.info("Activating purchase price with id: {}", id);
        Optional<PurchasePrices> purchasePrice = this.purchasePricesService.activatePrice(id);
        return purchasePrice.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Menonaktifkan harga beli.
     *
     * @param id ID harga beli
     * @return ResponseEntity dengan harga beli yang telah dinonaktifkan
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<PurchasePrices> deactivatePurchasePrice(@PathVariable UUID id) {
        log.info("Deactivating purchase price with id: {}", id);
        Optional<PurchasePrices> purchasePrice = this.purchasePricesService.deactivatePrice(id);
        return purchasePrice.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Soft delete harga beli.
     *
     * @param id ID harga beli
     * @return ResponseEntity dengan status operasi
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<PurchasePrices> deletePurchasePrice(@PathVariable UUID id) {
        log.info("Deleting purchase price with id: {}", id);
        Optional<PurchasePrices> purchasePrice = this.purchasePricesService.deletePrice(id);
        return purchasePrice.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Menghitung jumlah harga beli berdasarkan voucher.
     *
     * @param voucherId ID voucher
     * @return ResponseEntity dengan jumlah harga beli
     */
    @GetMapping("/count/voucher/{voucherId}")
    public ResponseEntity<Long> countPurchasePricesByVoucher(@PathVariable UUID voucherId) {
        log.info("Counting purchase prices by voucher id: {}", voucherId);
        Optional<Vouchers> voucher = this.vouchersService.findById(voucherId);
        if (voucher.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        long count = this.purchasePricesService.countByVouchers(voucher.get());
        return ResponseEntity.ok(count);
    }
}
