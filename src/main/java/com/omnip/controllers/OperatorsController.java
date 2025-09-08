package com.omnip.controllers;

import com.omnip.entities.Operators;
import com.omnip.services.OperatorsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller untuk menangani operasi-operasi terkait Operators.
 * Menyediakan REST API endpoints untuk manajemen operator.
 */
@Slf4j
@RestController
@RequestMapping("/api/operators")
@RequiredArgsConstructor
public class OperatorsController {

    private final OperatorsService operatorsService;

    /**
     * Mendapatkan semua operator.
     *
     * @return ResponseEntity dengan list operator
     */
    @GetMapping
    @PreAuthorize("hasRole('ROLE_omnip-store-admin') or hasRole('ROLE_omnip-store-user')")
    public ResponseEntity<List<Operators>> getAllOperators() {
        log.info("Getting all operators");
        List<Operators> operators = this.operatorsService.findAll();
        return ResponseEntity.ok(operators);
    }

    /**
     * Mendapatkan operator berdasarkan ID.
     *
     * @param id ID operator
     * @return ResponseEntity dengan operator yang ditemukan
     */
    @GetMapping("/{id}")
    public ResponseEntity<Operators> getOperatorById(@PathVariable UUID id) {
        log.info("Getting operator by id: {}", id);
        Optional<Operators> operator = this.operatorsService.findById(id);
        return operator.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mendapatkan operator berdasarkan kode.
     *
     * @param code kode operator
     * @return ResponseEntity dengan operator yang ditemukan
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Operators> getOperatorByCode(@PathVariable String code) {
        log.info("Getting operator by code: {}", code);
        Optional<Operators> operator = this.operatorsService.findByCode(code);
        return operator.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mendapatkan operator berdasarkan nama.
     *
     * @param name nama operator
     * @return ResponseEntity dengan operator yang ditemukan
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<Operators> getOperatorByName(@PathVariable String name) {
        log.info("Getting operator by name: {}", name);
        Optional<Operators> operator = this.operatorsService.findByName(name);
        return operator.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mencari operator berdasarkan kata kunci dalam nama.
     *
     * @param keyword kata kunci untuk pencarian
     * @return ResponseEntity dengan list operator
     */
    @GetMapping("/search/name")
    public ResponseEntity<List<Operators>> searchOperatorsByName(@RequestParam String keyword) {
        log.info("Searching operators by name containing: {}", keyword);
        List<Operators> operators = this.operatorsService.findByNameContaining(keyword);
        return ResponseEntity.ok(operators);
    }

    /**
     * Mencari operator berdasarkan kata kunci dalam kode.
     *
     * @param keyword kata kunci untuk pencarian
     * @return ResponseEntity dengan list operator
     */
    @GetMapping("/search/code")
    public ResponseEntity<List<Operators>> searchOperatorsByCode(@RequestParam String keyword) {
        log.info("Searching operators by code containing: {}", keyword);
        List<Operators> operators = this.operatorsService.findByCodeContaining(keyword);
        return ResponseEntity.ok(operators);
    }

    /**
     * Mendapatkan operator aktif.
     *
     * @return ResponseEntity dengan list operator aktif
     */
    @GetMapping("/active")
    public ResponseEntity<List<Operators>> getActiveOperators() {
        log.info("Getting active operators");
        List<Operators> operators = this.operatorsService.findActiveOperators();
        return ResponseEntity.ok(operators);
    }

    /**
     * Membuat operator baru.
     *
     * @param operators entity operator yang akan dibuat
     * @return ResponseEntity dengan operator yang telah dibuat
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<Operators> createOperator(@RequestBody Operators operators) {
        log.info("Creating new operator with code: {}", operators.getCode());
        try {
            // Validate operator data
            if (!this.operatorsService.validateOperator(operators)) {
                return ResponseEntity.badRequest().build();
            }

            Operators savedOperator = this.operatorsService.save(operators);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedOperator);
        } catch (Exception e) {
            log.error("Error creating operator: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mengupdate operator yang sudah ada.
     *
     * @param id        ID operator yang akan diupdate
     * @param operators entity operator dengan data baru
     * @return ResponseEntity dengan operator yang telah diupdate
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<Operators> updateOperator(@PathVariable UUID id, @RequestBody Operators operators) {
        log.info("Updating operator with id: {}", id);
        try {
            Optional<Operators> existingOperator = this.operatorsService.findById(id);
            if (existingOperator.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            operators.setId(id);

            // Validate operator data
            if (!this.operatorsService.validateOperator(operators)) {
                return ResponseEntity.badRequest().build();
            }

            Operators updatedOperator = this.operatorsService.save(operators);
            return ResponseEntity.ok(updatedOperator);
        } catch (Exception e) {
            log.error("Error updating operator: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mengaktifkan operator.
     *
     * @param id ID operator
     * @return ResponseEntity dengan operator yang telah diaktifkan
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<Operators> activateOperator(@PathVariable UUID id) {
        log.info("Activating operator with id: {}", id);
        Optional<Operators> operator = this.operatorsService.activateOperator(id);
        return operator.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Menonaktifkan operator.
     *
     * @param id ID operator
     * @return ResponseEntity dengan operator yang telah dinonaktifkan
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ROLE_omnip-admin')")
    public ResponseEntity<Operators> deactivateOperator(@PathVariable UUID id) {
        log.info("Deactivating operator with id: {}", id);
        Optional<Operators> operator = this.operatorsService.deactivateOperator(id);
        return operator.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Memeriksa apakah operator dengan kode tertentu sudah ada.
     *
     * @param code kode operator
     * @return ResponseEntity dengan status keberadaan
     */
    @GetMapping("/exists/code/{code}")
    public ResponseEntity<Boolean> checkOperatorExistsByCode(@PathVariable String code) {
        log.info("Checking if operator exists by code: {}", code);
        boolean exists = this.operatorsService.existsByCode(code);
        return ResponseEntity.ok(exists);
    }

    /**
     * Memeriksa apakah operator dengan nama tertentu sudah ada.
     *
     * @param name nama operator
     * @return ResponseEntity dengan status keberadaan
     */
    @GetMapping("/exists/name/{name}")
    public ResponseEntity<Boolean> checkOperatorExistsByName(@PathVariable String name) {
        log.info("Checking if operator exists by name: {}", name);
        boolean exists = this.operatorsService.existsByName(name);
        return ResponseEntity.ok(exists);
    }

    /**
     * Menghitung jumlah operator aktif.
     *
     * @return ResponseEntity dengan jumlah operator aktif
     */
    @GetMapping("/count/active")
    public ResponseEntity<Long> countActiveOperators() {
        log.info("Counting active operators");
        long count = this.operatorsService.countActiveOperators();
        return ResponseEntity.ok(count);
    }
}
