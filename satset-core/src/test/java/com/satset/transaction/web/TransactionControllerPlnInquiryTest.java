package com.satset.transaction.web;

import com.satset.shared.dto.UserDTO;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.dto.PlnInquiryDTO;
import com.satset.transaction.dto.PlnInquiryRequest;
import com.satset.transaction.service.postpaid.PostpaidService;
import com.satset.transaction.service.prepaid.PlnInquiryService;
import com.satset.transaction.service.topup.TransactionDomainService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionControllerPlnInquiryTest {

    private final TransactionDomainService txService = mock(TransactionDomainService.class);
    private final WalletGateway walletGateway = mock(WalletGateway.class);
    private final PostpaidService postpaidService = mock(PostpaidService.class);
    private final PlnInquiryService plnInquiryService = mock(PlnInquiryService.class);
    private final UserDTO userDTO = mock(UserDTO.class);

    private final TransactionController controller =
            new TransactionController(txService, walletGateway, postpaidService, plnInquiryService, userDTO);

    @Test
    void plnInquiryDelegatesToPlnInquiryService() {
        PlnInquiryDTO dto = new PlnInquiryDTO("BUDI SANTOSO", "12345678901", "R1 /000001300");
        when(plnInquiryService.inquiry("530000000001")).thenReturn(dto);

        var response = controller.plnInquiry(new PlnInquiryRequest("530000000001"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(dto);
    }
}
