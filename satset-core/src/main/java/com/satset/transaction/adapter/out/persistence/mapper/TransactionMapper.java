package com.satset.transaction.adapter.out.persistence.mapper;

import com.satset.transaction.adapter.out.persistence.entity.TransactionJpaEntity;
import com.satset.transaction.domain.model.Transactions;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.Optional;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    Transactions toDomain(TransactionJpaEntity entity);

    TransactionJpaEntity toEntity(Transactions transaction);

    default Optional<Transactions> toOptionalDomain(Optional<TransactionJpaEntity> entity) {
        return entity.map(this::toDomain);
    }

    default Page<Transactions> toDomainPage(Page<TransactionJpaEntity> entityPage) {
        return entityPage.map(this::toDomain);
    }
}
