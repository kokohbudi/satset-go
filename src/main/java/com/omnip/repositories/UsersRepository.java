package com.omnip.repositories;

import com.omnip.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    Users findByEmail(String email);

    boolean existsByReferalId(String referalId);
}
