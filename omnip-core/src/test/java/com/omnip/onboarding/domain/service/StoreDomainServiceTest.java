package com.omnip.onboarding.domain.service;

import com.omnip.onboarding.domain.model.Stores;
import com.omnip.onboarding.domain.port.out.StoreRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
