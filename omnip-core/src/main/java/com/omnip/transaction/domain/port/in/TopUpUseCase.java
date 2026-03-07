package com.omnip.transaction.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface TopUpUseCase {

    void topUp(UUID storeId, BigDecimal amount, String description);
}
