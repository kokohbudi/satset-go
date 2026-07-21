package com.satset.transaction.web;

import com.satset.shared.dto.UserDTO;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.dto.InquiryDTO;
import com.satset.transaction.dto.InquiryRequest;
import com.satset.transaction.service.postpaid.PostpaidService;
import com.satset.transaction.service.topup.TransactionDomainService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionControllerPostpaidTest {

    private static final UUID DENOM_ID = UUID.randomUUID();

    private final TransactionDomainService txService = mock(TransactionDomainService.class);
    private final WalletGateway walletGateway = mock(WalletGateway.class);
    private final PostpaidService postpaidService = mock(PostpaidService.class);
    private final UserDTO userDTO = mock(UserDTO.class);

    private final TransactionController controller =
            new TransactionController(txService, walletGateway, postpaidService, userDTO);

    @Test
    void inquiryDelegatesToPostpaidService() throws Exception {
        InquiryDTO dto = new InquiryDTO("BUDI SANTOSO", new BigDecimal("145000"),
                new BigDecimal("2500"), new BigDecimal("1500"), new BigDecimal("149000"), null);
        when(postpaidService.inquiry(DENOM_ID, "530000000001", null)).thenReturn(dto);

        var response = controller.inquiry(new InquiryRequest(DENOM_ID, "530000000001", null));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(dto);
    }
}
