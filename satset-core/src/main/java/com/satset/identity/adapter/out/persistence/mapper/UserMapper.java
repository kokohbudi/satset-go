package com.satset.identity.adapter.out.persistence.mapper;

import com.satset.identity.adapter.out.persistence.entity.UserJpaEntity;
import com.satset.identity.domain.model.Users;
import com.satset.shared.dto.UserDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserMapper {

    public Users toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Users user = new Users();
        user.setId(entity.getId());
        user.setEmail(entity.getEmail());
        user.setUsername(entity.getUsername());
        user.setFullname(entity.getFullname());
        user.setRegistrationChannel(entity.getRegistrationChannel());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());
        user.setUpdatedBy(entity.getUpdatedBy());
        user.setCreatedBy(entity.getCreatedBy());
        user.setActive(entity.isActive());
        user.setDeleted(entity.isDeleted());
        user.setProviderUserId(entity.getProviderUserId());
        user.setVersion(entity.getVersion());
        user.setStoreId(entity.getStoreId());
        user.setRoles(entity.getRoles());
        return user;
    }

    public UserJpaEntity toEntity(Users user) {
        if (user == null) {
            return null;
        }
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setUsername(user.getUsername());
        entity.setFullname(user.getFullname());
        entity.setRegistrationChannel(user.getRegistrationChannel());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        entity.setUpdatedBy(user.getUpdatedBy());
        entity.setCreatedBy(user.getCreatedBy());
        entity.setActive(user.isActive());
        entity.setDeleted(user.isDeleted());
        entity.setProviderUserId(user.getProviderUserId());
        entity.setVersion(user.getVersion());
        entity.setStoreId(user.getStoreId());
        entity.setRoles(user.getRoles());
        return entity;
    }

    public UserDTO toDTO(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setEmail(entity.getEmail());
        dto.setUsername(entity.getUsername());
        dto.setFullname(entity.getFullname());
        dto.setStoreId(entity.getStoreId());
        dto.setRoles(entity.getRoles());
        dto.setProviderUserId(entity.getProviderUserId());
        dto.setActive(entity.isActive());
        return dto;
    }

    public Optional<Users> toOptionalDomain(Optional<UserJpaEntity> entity) {
        return entity.map(this::toDomain);
    }

    public List<Users> toDomainList(List<UserJpaEntity> entities) {
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }
}