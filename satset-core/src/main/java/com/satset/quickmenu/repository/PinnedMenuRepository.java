package com.satset.quickmenu.repository;

import com.satset.quickmenu.model.PinnedMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Pin persistence. Spring Data provides the implementation. */
@Repository
public interface PinnedMenuRepository extends JpaRepository<PinnedMenu, UUID> {

    List<PinnedMenu> findByUserId(String userId);

    boolean existsByUserIdAndRoleName(String userId, String roleName);

    long deleteByUserIdAndRoleName(String userId, String roleName);
}
