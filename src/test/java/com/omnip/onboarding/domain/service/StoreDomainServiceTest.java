package com.omnip.onboarding.domain.service;

import com.omnip.onboarding.domain.model.Stores;
import com.omnip.onboarding.domain.port.out.StoreRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreDomainServiceTest {

    @Mock
    private StoreRepositoryPort storeRepository;

    @Mock
    private StoreHelper storeBusiness;

    @InjectMocks
    private StoreDomainService storeDomainService;

    @Test
    void createNewStore_DelegatesToHelperThenRepository() {
        Stores input = new Stores();
        Stores prepared = new Stores();
        Stores saved = new Stores();
        saved.setId(UUID.randomUUID());

        when(storeBusiness.prepareNewStore(input)).thenReturn(prepared);
        when(storeRepository.save(prepared)).thenReturn(saved);

        Stores result = storeDomainService.createNewStore(input);

        assertSame(saved, result);
        verify(storeBusiness).prepareNewStore(input);
        verify(storeRepository).save(prepared);
    }

    @Test
    void createNewStore_ReturnsRepositoryResult_NotHelper() {
        Stores input = new Stores();
        Stores prepared = new Stores();
        Stores saved = new Stores();
        saved.setName("Saved Store");

        when(storeBusiness.prepareNewStore(any())).thenReturn(prepared);
        when(storeRepository.save(any())).thenReturn(saved);

        Stores result = storeDomainService.createNewStore(input);

        assertEquals("Saved Store", result.getName());
    }
}
