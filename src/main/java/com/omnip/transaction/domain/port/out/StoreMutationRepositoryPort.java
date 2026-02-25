package com.omnip.transaction.domain.port.out;

import com.omnip.transaction.domain.model.StoreMutations;

public interface StoreMutationRepositoryPort {

    StoreMutations save(StoreMutations mutation);
}
