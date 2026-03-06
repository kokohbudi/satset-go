package com.omnip.identity.adapter.out.persistence;

import com.omnip.identity.adapter.out.persistence.entity.UserJpaEntity;
import com.omnip.identity.adapter.out.persistence.mapper.UserMapper;
import com.omnip.identity.domain.model.Users;
import com.omnip.identity.domain.port.out.UserRepositoryPort;
import com.omnip.onboarding.domain.port.out.OnboardingUserPort;
import com.omnip.shared.dto.UserDTO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
class UserRepositoryAdapter implements UserRepositoryPort, OnboardingUserPort {

    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;

    UserRepositoryAdapter(UserJpaRepository jpaRepository, UserMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Users save(Users user) {
        UserJpaEntity entity = mapper.toEntity(user);
        UserJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Users findByEmail(String email) {
        return mapper.toDomain(jpaRepository.findByEmail(email));
    }

    @Override
    public UserDTO findByEmailDTO(String email) {
        return mapper.toDTO(jpaRepository.findByEmail(email));
    }

    @Override
    public Users findByProviderUserId(String providerUserId) {
        return mapper.toDomain(jpaRepository.findByProviderUserId(providerUserId));
    }

    @Override
    public UUID findStoreIdByProviderUserId(String providerUserId) {
        UserJpaEntity entity = jpaRepository.findByProviderUserId(providerUserId);
        return entity != null ? entity.getStoreId() : null;
    }

    @Override
    public List<Users> findByEmailInAndStoreId(List<String> emails, String storeId) {
        return mapper.toDomainList(jpaRepository.findByEmailInAndStoreId(emails, storeId));
    }
}