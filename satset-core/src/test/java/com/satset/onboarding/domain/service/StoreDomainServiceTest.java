package com.satset.onboarding.domain.service;

import com.satset.onboarding.domain.model.Stores;
import com.satset.onboarding.domain.port.out.StoreRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreDomainServiceTest {

    @Mock
    private StoreRepositoryPort storeRepository;

    @InjectMocks
    private StoreDomainService storeDomainService;

    @Test
    void createNewStore_DelegatesToRepository() {
        Stores input = new Stores();
        Stores saved = new Stores();
        saved.setId(UUID.randomUUID());
        saved.setName("Saved Store");

        when(storeRepository.save(input)).thenReturn(saved);

        Stores result = storeDomainService.createNewStore(input);

        assertThat(result).isSameAs(saved);
        assertThat(result.getName()).isEqualTo("Saved Store");
        verify(storeRepository).save(input);
    }
}
