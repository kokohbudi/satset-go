package com.satset.transaction.service.prepaid;

import com.satset.shared.exception.SupplierException;
import com.satset.shared.logging.LogContext;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.dto.PlnInquiryDTO;
import com.satset.transaction.model.PlnInquiryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Prepaid PLN customer-name inquiry (Digiflazz inquiry-pln). Validates a PLN customer_no and
 * surfaces the account holder name before the user buys a prepaid token via the normal
 * {@code /api/transactions/purchase} flow. No bill, no postpaid pay here — see
 * {@link com.satset.transaction.service.postpaid.PostpaidService} for that (untouched, different feature).
 */
@Slf4j
@Service
@LogContext("PlnInquiry")
public class PlnInquiryService {

    private final ProviderPort providerPort;

    public PlnInquiryService(ProviderPort providerPort) {
        this.providerPort = providerPort;
    }

    public PlnInquiryDTO inquiry(String customerNo) {
        PlnInquiryResult r = providerPort.plnInquiry(customerNo);
        if (!r.ok()) {
            throw supplierError(r.rc(), r.message(), "PlnInquiry");
        }
        return new PlnInquiryDTO(r.customerName(), r.meterNo(), r.segmentPower());
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
