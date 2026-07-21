package com.satset.transaction.service.postpaid;

import com.satset.catalog.model.DenomType;
import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.shared.exception.SupplierException;
import com.satset.shared.logging.LogContext;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.dto.InquiryDTO;
import com.satset.transaction.dto.TransactionDTO;
import com.satset.transaction.model.InquiryResult;
import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.TransactionStatus;
import com.satset.transaction.model.Transactions;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.service.topup.RefNoGenerator;
import com.satset.transaction.service.topup.TransactionDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Pascabayar (postpaid) inquiry + pay.
 */
@Slf4j
@Service
@LogContext("Postpaid")
public class PostpaidService {

    private final DenomRepository denomRepository;
    private final WalletGateway walletGateway;
    private final ProviderPort providerPort;
    private final RefNoGenerator refNoGenerator;
    private final TransactionRepository transactionRepository;
    private final TransactionDomainService transactionDomainService;

    public PostpaidService(DenomRepository denomRepository, WalletGateway walletGateway,
            ProviderPort providerPort, RefNoGenerator refNoGenerator,
            TransactionRepository transactionRepository, TransactionDomainService transactionDomainService) {
        this.denomRepository = denomRepository;
        this.walletGateway = walletGateway;
        this.providerPort = providerPort;
        this.refNoGenerator = refNoGenerator;
        this.transactionRepository = transactionRepository;
        this.transactionDomainService = transactionDomainService;
    }

    public InquiryDTO inquiry(UUID denomId, String customerNo, BigDecimal amount) throws BusinessException {
        DenomInfo denom = loadPostpaidDenom(denomId);
        validateAmountRule(denom, amount);
        String ref = refNoGenerator.next();
        InquiryResult r = providerPort.inquiry(customerNo, denom.code(), ref, amount);
        if (!r.ok()) {
            throw supplierError(r.rc(), r.message(), "Inquiry");
        }
        BigDecimal markup = denom.adminFee() != null ? denom.adminFee() : BigDecimal.ZERO;
        BigDecimal total = r.bill().add(r.admin()).add(markup);
        return new InquiryDTO(r.customerName(), r.bill(), r.admin(), markup, total, r.desc());
    }

    /**
     * Pascabayar money path: re-inquiry (fresh bill, never trust the client-supplied one) →
     * mismatch guard → charge → provider pay → reconcile. Ordering is the safety contract:
     * no DB row and no wallet deduction happen until the bill is re-confirmed against
     * {@code expectedTotal}.
     */
    public TransactionDTO pay(UUID storeId, String walletId, UUID denomId, String customerNo,
            BigDecimal amount, BigDecimal expectedTotal) throws BusinessException {
        DenomInfo denom = loadPostpaidDenom(denomId);
        validateAmountRule(denom, amount);

        boolean duplicate = transactionRepository
                .existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
                        storeId, denomId, customerNo,
                        List.of(TransactionStatus.PENDING, TransactionStatus.PROCESSING,
                                TransactionStatus.SUCCESS),
                        LocalDateTime.now().minusMinutes(1));
        if (duplicate) {
            throw new BusinessException("DUPLICATE_TRANSACTION",
                    "Transaksi serupa baru saja dibuat. Tunggu 1 menit sebelum mencoba lagi.");
        }

        String ref = refNoGenerator.next();
        InquiryResult inqNow = providerPort.inquiry(customerNo, denom.code(), ref, amount);
        if (!inqNow.ok()) {
            throw supplierError(inqNow.rc(), inqNow.message(), "Pay re-inquiry");
        }

        BigDecimal markup = denom.adminFee() != null ? denom.adminFee() : BigDecimal.ZERO;
        BigDecimal total = inqNow.bill().add(inqNow.admin()).add(markup);
        if (expectedTotal.compareTo(total) != 0) {
            throw new BusinessException("BILL_CHANGED",
                    "Tagihan berubah sejak pengecekan. Silakan cek tagihan ulang.");
        }

        Transactions tx = new Transactions();
        tx.setStoreId(storeId);
        tx.setWalletId(walletId);
        tx.setProductDenomId(denomId);
        tx.setDenomName(denom.name());
        tx.setProductName(denom.productName());
        tx.setTargetNumber(customerNo);
        tx.setPrice(inqNow.bill());
        tx.setAdminFee(inqNow.admin().add(markup));
        tx.setTotal(total);
        tx.setStatus(TransactionStatus.PROCESSING);
        tx.setRefNo(ref);
        tx.setCustomerName(inqNow.customerName());
        tx = transactionRepository.save(tx);

        walletGateway.deductBalance(walletId, total, tx.getId(),
                "Pembayaran " + denom.productName() + " " + customerNo);

        ProviderResponse payResp = providerPort.payPostpaid(customerNo, denom.code(), ref);
        transactionDomainService.reconcileProviderResult(tx, payResp, walletId, denom);

        Transactions settled = transactionRepository.findById(tx.getId()).orElse(tx);
        return toDTO(settled);
    }

    private TransactionDTO toDTO(Transactions tx) {
        return new TransactionDTO(tx.getId(), tx.getRefNo(), tx.getStoreId(), tx.getTargetNumber(),
                tx.getCustomerName(), tx.getDenomName(), tx.getProductName(), tx.getPrice(),
                tx.getAdminFee(), tx.getTotal(), tx.getStatus(), tx.getProviderRef(),
                tx.getSerialNumber(), tx.getCreatedAt());
    }

    private DenomInfo loadPostpaidDenom(UUID denomId) throws BusinessException {
        DenomInfo denom = denomRepository.findDenomInfoById(denomId)
                .orElseThrow(() -> new ResourceNotFoundException("Denom", denomId));
        if (!denom.requiresInquiry()) {
            throw new BusinessException("NOT_POSTPAID", "Produk ini bukan produk pascabayar");
        }
        if (!denom.isAvailable()) {
            throw new BusinessException("DENOM_UNAVAILABLE", "Produk sedang tidak tersedia");
        }
        return denom;
    }

    private void validateAmountRule(DenomInfo denom, BigDecimal amount) throws BusinessException {
        if (denom.denomType() == DenomType.OPEN_AMOUNT) {
            if (amount == null) {
                throw new BusinessException("AMOUNT_REQUIRED", "Nominal wajib diisi untuk produk ini");
            }
            boolean belowMin = denom.minAmount() != null && amount.compareTo(denom.minAmount()) < 0;
            boolean aboveMax = denom.maxAmount() != null && amount.compareTo(denom.maxAmount()) > 0;
            if (belowMin || aboveMax) {
                throw new BusinessException("AMOUNT_OUT_OF_RANGE", "Nominal di luar batas yang diizinkan");
            }
        } else if (amount != null) {
            throw new BusinessException("AMOUNT_NOT_ALLOWED", "Produk ini membayar tagihan penuh tanpa nominal");
        }
    }

    /**
     * Build a client-safe {@link SupplierException} from a failed supplier result. Transport/parse
     * failures ({@code rc} HTTP/PARSE) carry raw exception text — never leak that to the client;
     * substitute a generic message and keep the detail in the log. A numeric Digiflazz rc is a real
     * business message (e.g. "Nomor tidak ditemukan") and is safe to surface.
     */
    private SupplierException supplierError(String rc, String rawMessage, String context) {
        log.error("{} gagal — supplier rc={} detail={}", context, rc, rawMessage);
        String clientMsg = ("HTTP".equals(rc) || "PARSE".equals(rc))
                ? "Layanan supplier sedang tidak tersedia. Coba lagi sebentar."
                : rawMessage;
        return new SupplierException(rc, clientMsg);
    }
}
