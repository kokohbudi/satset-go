package com.omnip.controllers;

import com.omnip.entities.Vouchers;
import com.omnip.entities.Operators;
import com.omnip.services.VouchersService;
import com.omnip.services.OperatorsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller untuk menangani operasi-operasi terkait Vouchers.
 * Menyediakan REST API endpoints untuk manajemen voucher.
 */
@Slf4j
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VouchersController {

    private final VouchersService vouchersService;
    private final OperatorsService operatorsService;

    /**
     * Mendapatkan semua voucher.
     * 
     * @return ResponseEntity dengan list voucher
     */
    @GetMapping
    public ResponseEntity<List<Vouchers>> getAllVouchers() {
        log.info("Getting all vouchers");
        List<Vouchers> vouchers = vouchersService.findAll();
        return ResponseEntity.ok(vouchers);
    }

    /**
     * Mendapatkan voucher berdasarkan ID.
     * 
     * @param id ID voucher
     * @return ResponseEntity dengan voucher yang ditemukan
     */
    @GetMapping("/{id}")
    public ResponseEntity<Vouchers> getVoucherById(@PathVariable UUID id) {
        log.info("Getting voucher by id: {}", id);
        Optional<Vouchers> voucher = vouchersService.findById(id);
        return voucher.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mendapatkan voucher berdasarkan kode.
     * 
     * @param code kode voucher
     * @return ResponseEntity dengan voucher yang ditemukan
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Vouchers> getVoucherByCode(@PathVariable String code) {
        log.info("Getting voucher by code: {}", code);
        Optional<Vouchers> voucher = vouchersService.findByCode(code);
        return voucher.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mendapatkan voucher berdasarkan operator ID.
     * 
     * @param operatorId ID operator
     * @return ResponseEntity dengan list voucher
     */
    @GetMapping("/operator/{operatorId}")
    public ResponseEntity<List<Vouchers>> getVouchersByOperatorId(@PathVariable UUID operatorId) {
        log.info("Getting vouchers by operator id: {}", operatorId);
        List<Vouchers> vouchers = vouchersService.findByOperatorId(operatorId);
        return ResponseEntity.ok(vouchers);
    }

    /**
     * Mendapatkan voucher aktif.
     * 
     * @return ResponseEntity dengan list voucher aktif
     */
    @GetMapping("/active")
    public ResponseEntity<List<Vouchers>> getActiveVouchers() {
        log.info("Getting active vouchers");
        List<Vouchers> vouchers = vouchersService.findActiveVouchers();
        return ResponseEntity.ok(vouchers);
    }

    /**
     * Mendapatkan voucher berdasarkan range denominasi.
     * 
     * @param min denominasi minimum
     * @param max denominasi maksimum
     * @return ResponseEntity dengan list voucher
     */
    @GetMapping("/denomination")
    public ResponseEntity<List<Vouchers>> getVouchersByDenominationRange(
            @RequestParam BigDecimal min, 
            @RequestParam BigDecimal max) {
        log.info("Getting vouchers by denomination range: {} - {}", min, max);
        List<Vouchers> vouchers = vouchersService.findByDenominationRange(min, max);
        return ResponseEntity.ok(vouchers);
    }

    /**
     * Membuat voucher baru.
     * 
     * @param vouchers entity voucher yang akan dibuat
     * @return ResponseEntity dengan voucher yang telah dibuat
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<Vouchers> createVoucher(@RequestBody Vouchers vouchers) {
        log.info("Creating new voucher with code: {}", vouchers.getCode());
        try {
            // Validate operator exists if provided
            if (vouchers.getOperator() != null && vouchers.getOperator().getId() != null) {
                Optional<Operators> operator = operatorsService.findById(vouchers.getOperator().getId());
                if (operator.isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                vouchers.setOperator(operator.get());
            }
            
            Vouchers savedVoucher = vouchersService.save(vouchers);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedVoucher);
        } catch (Exception e) {
            log.error("Error creating voucher: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mengupdate voucher yang sudah ada.
     * 
     * @param id ID voucher yang akan diupdate
     * @param vouchers entity voucher dengan data baru
     * @return ResponseEntity dengan voucher yang telah diupdate
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<Vouchers> updateVoucher(@PathVariable UUID id, @RequestBody Vouchers vouchers) {
        log.info("Updating voucher with id: {}", id);
        try {
            Optional<Vouchers> existingVoucher = vouchersService.findById(id);
            if (existingVoucher.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            vouchers.setId(id);
            
            // Validate operator exists if provided
            if (vouchers.getOperator() != null && vouchers.getOperator().getId() != null) {
                Optional<Operators> operator = operatorsService.findById(vouchers.getOperator().getId());
                if (operator.isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                vouchers.setOperator(operator.get());
            }
            
            Vouchers updatedVoucher = vouchersService.save(vouchers);
            return ResponseEntity.ok(updatedVoucher);
        } catch (Exception e) {
            log.error("Error updating voucher: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mengaktifkan voucher.
     * 
     * @param id ID voucher
     * @return ResponseEntity dengan voucher yang telah diaktifkan
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<Vouchers> activateVoucher(@PathVariable UUID id) {
        log.info("Activating voucher with id: {}", id);
        Optional<Vouchers> voucher = vouchersService.activateVoucher(id);
        return voucher.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Menonaktifkan voucher.
     * 
     * @param id ID voucher
     * @return ResponseEntity dengan voucher yang telah dinonaktifkan
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<Vouchers> deactivateVoucher(@PathVariable UUID id) {
        log.info("Deactivating voucher with id: {}", id);
        Optional<Vouchers> voucher = vouchersService.deactivateVoucher(id);
        return voucher.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Soft delete voucher.
     * 
     * @param id ID voucher
     * @return ResponseEntity dengan status operasi
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<Vouchers> deleteVoucher(@PathVariable UUID id) {
        log.info("Deleting voucher with id: {}", id);
        Optional<Vouchers> voucher = vouchersService.deleteVoucher(id);
        return voucher.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Menghitung jumlah voucher berdasarkan operator.
     * 
     * @param operatorId ID operator
     * @return ResponseEntity dengan jumlah voucher
     */
    @GetMapping("/count/operator/{operatorId}")
    public ResponseEntity<Long> countVouchersByOperator(@PathVariable UUID operatorId) {
        log.info("Counting vouchers by operator id: {}", operatorId);
        Optional<Operators> operator = operatorsService.findById(operatorId);
        if (operator.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        long count = vouchersService.countByOperator(operator.get());
        return ResponseEntity.ok(count);
    }
}
