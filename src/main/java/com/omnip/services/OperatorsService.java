package com.omnip.services;

import com.omnip.entities.Operators;
import com.omnip.repositories.OperatorsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class untuk menangani business logic terkait Operators.
 * Menyediakan operasi CRUD dan business logic untuk entity Operators.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OperatorsService {

    private final OperatorsRepository operatorsRepository;

    /**
     * Menyimpan operator baru atau mengupdate operator yang sudah ada.
     * 
     * @param operators entity operator yang akan disimpan
     * @return operator yang telah disimpan
     */
    public Operators save(Operators operators) {
        log.info("Saving operator with code: {}", operators.getCode());
        return operatorsRepository.save(operators);
    }

    /**
     * Mencari operator berdasarkan ID.
     * 
     * @param id ID operator
     * @return Optional operator yang ditemukan
     */
    @Transactional(readOnly = true)
    public Optional<Operators> findById(UUID id) {
        log.debug("Finding operator by id: {}", id);
        return operatorsRepository.findById(id);
    }

    /**
     * Mencari operator berdasarkan kode.
     * 
     * @param code kode operator
     * @return Optional operator yang ditemukan
     */
    @Transactional(readOnly = true)
    public Optional<Operators> findByCode(String code) {
        log.debug("Finding operator by code: {}", code);
        return operatorsRepository.findByCode(code);
    }

    /**
     * Mencari operator berdasarkan nama.
     * 
     * @param name nama operator
     * @return Optional operator yang ditemukan
     */
    @Transactional(readOnly = true)
    public Optional<Operators> findByName(String name) {
        log.debug("Finding operator by name: {}", name);
        return operatorsRepository.findByName(name);
    }

    /**
     * Mencari semua operator.
     * 
     * @return List semua operator
     */
    @Transactional(readOnly = true)
    public List<Operators> findAll() {
        log.debug("Finding all operators");
        return operatorsRepository.findAll();
    }

    /**
     * Mencari operator berdasarkan kata kunci dalam nama.
     * 
     * @param keyword kata kunci untuk pencarian
     * @return List operator yang ditemukan
     */
    @Transactional(readOnly = true)
    public List<Operators> findByNameContaining(String keyword) {
        log.debug("Finding operators by name containing: {}", keyword);
        return operatorsRepository.findByNameContainingIgnoreCase(keyword);
    }

    /**
     * Mencari operator berdasarkan kata kunci dalam kode.
     * 
     * @param keyword kata kunci untuk pencarian
     * @return List operator yang ditemukan
     */
    @Transactional(readOnly = true)
    public List<Operators> findByCodeContaining(String keyword) {
        log.debug("Finding operators by code containing: {}", keyword);
        return operatorsRepository.findByCodeContainingIgnoreCase(keyword);
    }

    /**
     * Mencari operator aktif.
     * 
     * @return List operator aktif
     */
    @Transactional(readOnly = true)
    public List<Operators> findActiveOperators() {
        log.debug("Finding active operators");
        return operatorsRepository.findByActiveTrue();
    }

    /**
     * Mencari operator berdasarkan kode dan status aktif.
     * 
     * @param code kode operator
     * @param active status aktif
     * @return Optional operator yang ditemukan
     */
    @Transactional(readOnly = true)
    public Optional<Operators> findByCodeAndActive(String code, boolean active) {
        log.debug("Finding operator by code: {} and active: {}", code, active);
        return operatorsRepository.findByCodeAndActive(code, active);
    }

    /**
     * Mencari operator berdasarkan nama dan status aktif.
     * 
     * @param name nama operator
     * @param active status aktif
     * @return Optional operator yang ditemukan
     */
    @Transactional(readOnly = true)
    public Optional<Operators> findByNameAndActive(String name, boolean active) {
        log.debug("Finding operator by name: {} and active: {}", name, active);
        return operatorsRepository.findByNameAndActive(name, active);
    }

    /**
     * Memeriksa apakah operator dengan kode tertentu sudah ada.
     * 
     * @param code kode operator
     * @return true jika sudah ada, false jika belum
     */
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        log.debug("Checking if operator exists by code: {}", code);
        return operatorsRepository.existsByCode(code);
    }

    /**
     * Memeriksa apakah operator dengan nama tertentu sudah ada.
     * 
     * @param name nama operator
     * @return true jika sudah ada, false jika belum
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        log.debug("Checking if operator exists by name: {}", name);
        return operatorsRepository.existsByName(name);
    }

    /**
     * Mengaktifkan operator.
     * 
     * @param id ID operator
     * @return operator yang telah diaktifkan
     */
    public Optional<Operators> activateOperator(UUID id) {
        log.info("Activating operator with id: {}", id);
        Optional<Operators> operatorOpt = operatorsRepository.findById(id);
        if (operatorOpt.isPresent()) {
            Operators operator = operatorOpt.get();
            operator.setActive(true);
            return Optional.of(operatorsRepository.save(operator));
        }
        return Optional.empty();
    }

    /**
     * Menonaktifkan operator.
     * 
     * @param id ID operator
     * @return operator yang telah dinonaktifkan
     */
    public Optional<Operators> deactivateOperator(UUID id) {
        log.info("Deactivating operator with id: {}", id);
        Optional<Operators> operatorOpt = operatorsRepository.findById(id);
        if (operatorOpt.isPresent()) {
            Operators operator = operatorOpt.get();
            operator.setActive(false);
            return Optional.of(operatorsRepository.save(operator));
        }
        return Optional.empty();
    }

    /**
     * Menghitung jumlah operator aktif.
     * 
     * @return jumlah operator aktif
     */
    @Transactional(readOnly = true)
    public long countActiveOperators() {
        log.debug("Counting active operators");
        return operatorsRepository.countActiveOperators();
    }

    /**
     * Memvalidasi operator sebelum menyimpan.
     * 
     * @param operators operator yang akan divalidasi
     * @return true jika valid, false jika tidak
     */
    public boolean validateOperator(Operators operators) {
        if (operators.getCode() == null || operators.getCode().trim().isEmpty()) {
            log.warn("Operator code cannot be null or empty");
            return false;
        }
        if (operators.getName() == null || operators.getName().trim().isEmpty()) {
            log.warn("Operator name cannot be null or empty");
            return false;
        }
        
        // Check for duplicate code (excluding current operator if updating)
        if (operators.getId() == null) {
            // New operator
            if (existsByCode(operators.getCode())) {
                log.warn("Operator with code {} already exists", operators.getCode());
                return false;
            }
        } else {
            // Updating existing operator
            Optional<Operators> existingOpt = findByCode(operators.getCode());
            if (existingOpt.isPresent() && !existingOpt.get().getId().equals(operators.getId())) {
                log.warn("Another operator with code {} already exists", operators.getCode());
                return false;
            }
        }
        
        return true;
    }
}
