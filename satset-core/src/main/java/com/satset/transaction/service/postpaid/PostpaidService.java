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
import com.satset.transaction.model.InquiryResult;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.service.topup.RefNoGenerator;
import com.satset.transaction.service.topup.TransactionDomainService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Pascabayar (postpaid) inquiry + pay. This task covers inquiry only — no DB row,
 * no balance deduction (see Task 9 for pay).
 */
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
            throw new SupplierException(r.rc(), r.message());
        }
        BigDecimal markup = denom.adminFee() != null ? denom.adminFee() : BigDecimal.ZERO;
        BigDecimal total = r.bill().add(r.admin()).add(markup);
        return new InquiryDTO(r.customerName(), r.bill(), r.admin(), markup, total, r.desc());
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
}
