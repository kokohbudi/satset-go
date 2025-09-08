package com.omnip.services;

import com.omnip.entities.Vouchers;
import com.omnip.entities.Operators;
import com.omnip.repositories.VouchersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class untuk menangani business logic terkait Vouchers.
 * Menyediakan operasi CRUD dan business logic untuk entity Vouchers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VouchersService {

    private final VouchersRepository vouchersRepository;

    /**
     * Menyimpan voucher baru atau mengupdate voucher yang sudah ada.
     * 
     * @param vouchers entity voucher yang akan disimpan
     * @return voucher yang telah disimpan
     */
    public Vouchers save(Vouchers vouchers) {
        log.info("Saving voucher with code: {}", vouchers.getCode());
        return vouchersRepository.save(vouchers);
    }

    /**
     * Mencari voucher berdasarkan ID.
     * 
     * @param id ID voucher
     * @return Optional voucher yang ditemukan
     */
    @Transactional(readOnly = true)
    public Optional<Vouchers> findById(UUID id) {
        log.debug("Finding voucher by id: {}", id);
        return vouchersRepository.findById(id);
    }

    /**
     * Mencari voucher berdasarkan kode.
     * 
     * @param code kode voucher
     * @return Optional voucher yang ditemukan
     */
    @Transactional(readOnly = true)
    public Optional<Vouchers> findByCode(String code) {
        log.debug("Finding voucher by code: {}", code);
        return vouchersRepository.findByCode(code);
    }

    /**
     * Mencari semua voucher.
     * 
     * @return List semua voucher
     */
    @Transactional(readOnly = true)
    public List<Vouchers> findAll() {
        log.debug("Finding all vouchers");
        return vouchersRepository.findAll();
    }

    /**
     * Mencari voucher berdasarkan operator.
     * 
     * @param operator operator yang terkait
     * @return List voucher yang ditemukan
     */
    @Transactional(readOnly = true)
    public List<Vouchers> findByOperator(Operators operator) {
        log.debug("Finding vouchers by operator: {}", operator.getName());
        return vouchersRepository.findByOperator(operator);
    }

    /**
     * Mencari voucher berdasarkan operator ID.
     * 
     * @param operatorId ID operator
     * @return List voucher yang ditemukan
     */
    @Transactional(readOnly = true)
    public List<Vouchers> findByOperatorId(UUID operatorId) {
        log.debug("Finding vouchers by operator id: {}", operatorId);
        return vouchersRepository.findByOperatorId(operatorId);
    }

    /**
     * Mencari voucher aktif berdasarkan operator.
     * 
     * @param operator operator yang terkait
     * @return List voucher aktif yang ditemukan
     */
    @Transactional(readOnly = true)
    public List<Vouchers> findActiveByOperator(Operators operator) {
        log.debug("Finding active vouchers by operator: {}", operator.getName());
        return vouchersRepository.findByOperatorAndActiveTrue(operator);
    }

    /**
     * Mencari voucher berdasarkan range denominasi.
     * 
     * @param minDenomination denominasi minimum
     * @param maxDenomination denominasi maksimum
     * @return List voucher yang ditemukan
     */
    @Transactional(readOnly = true)
    public List<Vouchers> findByDenominationRange(BigDecimal minDenomination, BigDecimal maxDenomination) {
        log.debug("Finding vouchers by denomination range: {} - {}", minDenomination, maxDenomination);
        return vouchersRepository.findByDenominationBetween(minDenomination, maxDenomination);
    }

    /**
     * Mencari voucher aktif yang belum dihapus.
     * 
     * @return List voucher aktif
     */
    @Transactional(readOnly = true)
    public List<Vouchers> findActiveVouchers() {
        log.debug("Finding active vouchers");
        return vouchersRepository.findByActiveTrueAndDeletedFalse();
    }

    /**
     * Mengaktifkan voucher.
     * 
     * @param id ID voucher
     * @return voucher yang telah diaktifkan
     */
    public Optional<Vouchers> activateVoucher(UUID id) {
        log.info("Activating voucher with id: {}", id);
        Optional<Vouchers> voucherOpt = vouchersRepository.findById(id);
        if (voucherOpt.isPresent()) {
            Vouchers voucher = voucherOpt.get();
            voucher.setActive(true);
            return Optional.of(vouchersRepository.save(voucher));
        }
        return Optional.empty();
    }

    /**
     * Menonaktifkan voucher.
     * 
     * @param id ID voucher
     * @return voucher yang telah dinonaktifkan
     */
    public Optional<Vouchers> deactivateVoucher(UUID id) {
        log.info("Deactivating voucher with id: {}", id);
        Optional<Vouchers> voucherOpt = vouchersRepository.findById(id);
        if (voucherOpt.isPresent()) {
            Vouchers voucher = voucherOpt.get();
            voucher.setActive(false);
            return Optional.of(vouchersRepository.save(voucher));
        }
        return Optional.empty();
    }

    /**
     * Soft delete voucher.
     * 
     * @param id ID voucher
     * @return voucher yang telah dihapus
     */
    public Optional<Vouchers> deleteVoucher(UUID id) {
        log.info("Soft deleting voucher with id: {}", id);
        Optional<Vouchers> voucherOpt = vouchersRepository.findById(id);
        if (voucherOpt.isPresent()) {
            Vouchers voucher = voucherOpt.get();
            voucher.setDeleted(true);
            voucher.setActive(false);
            return Optional.of(vouchersRepository.save(voucher));
        }
        return Optional.empty();
    }

    /**
     * Menghitung jumlah voucher berdasarkan operator.
     * 
     * @param operator operator yang terkait
     * @return jumlah voucher
     */
    @Transactional(readOnly = true)
    public long countByOperator(Operators operator) {
        log.debug("Counting vouchers by operator: {}", operator.getName());
        return vouchersRepository.countByOperator(operator);
    }
}
