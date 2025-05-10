package com.omnip.repositories;

import com.omnip.entities.Users;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    @Cacheable(value = "stores", key = "#email", cacheManager = "fastCacheManager")
    Users findByEmail(String email);

    @Query("SELECT u FROM Users u WHERE u.email IN :emails AND u.store.id = CAST(:storeId AS java.util.UUID)")
    List<Users> findByEmailInAndStoreId(@Param("emails") List<String> emails, @Param("storeId") String storeId);

    Users findByProviderUserId(String providerUserId);


}
