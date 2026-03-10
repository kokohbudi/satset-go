package com.satset.transaction.domain.port.in;

import java.math.BigDecimal;

public interface TopUpUseCase {

    void topUp(String walletId, BigDecimal amount, String description);
}
