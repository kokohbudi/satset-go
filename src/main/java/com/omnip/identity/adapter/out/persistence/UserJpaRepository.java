package com.omnip.identity.adapter.out.persistence;

import com.omnip.identity.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    UserJpaEntity findByEmail(String email);

    UserJpaEntity findByProviderUserId(String providerUserId);

    @Query("SELECT u FROM UserJpaEntity u WHERE u.email IN :emails AND u.storeId = CAST(:storeId AS java.util.UUID)")
    List<UserJpaEntity> findByEmailInAndStoreId(@Param("emails") List<String> emails, @Param("storeId") String storeId);

    List<UserJpaEntity> findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrFullnameContainingIgnoreCase(
            String email, String username, String fullname);
}