package com.satset.identity.adapter.out.persistence.mapper;

import com.satset.identity.adapter.out.persistence.entity.UserJpaEntity;
import com.satset.identity.domain.model.Users;
import com.satset.shared.dto.UserDTO;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface UserMapper {

    Users toDomain(UserJpaEntity entity);

    UserJpaEntity toEntity(Users user);

    UserDTO toDTO(UserJpaEntity entity);

    List<Users> toDomainList(List<UserJpaEntity> entities);

    default Optional<Users> toOptionalDomain(Optional<UserJpaEntity> entity) {
        return entity.map(this::toDomain);
    }
}
