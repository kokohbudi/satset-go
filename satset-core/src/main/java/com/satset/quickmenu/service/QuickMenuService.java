package com.satset.quickmenu.service;

import com.satset.quickmenu.model.MenuItem;
import com.satset.quickmenu.model.PinnedMenu;
import com.satset.quickmenu.repository.PinnedMenuRepository;
import com.satset.shared.dto.RoleInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Per-user dashboard quick-menu pin operations. */
@Service
public class QuickMenuService {

    private final PinnedMenuRepository repo;
    private final MenuFlattener flattener;

    public QuickMenuService(PinnedMenuRepository repo, MenuFlattener flattener) {
        this.repo = repo;
        this.flattener = flattener;
    }

    /**
     * Pin the role if not pinned, otherwise unpin it.
     * No access guard: an inaccessible pin simply never renders because
     * {@link #quickMenu} only returns pins present in the user's current menu.
     *
     * @return true if the role is now pinned, false if now unpinned
     */
    @Transactional
    public boolean toggle(String userId, String roleName) {
        if (repo.existsByUserIdAndRoleName(userId, roleName)) {
            repo.deleteByUserIdAndRoleName(userId, roleName);
            return false;
        }
        PinnedMenu pin = new PinnedMenu();
        pin.setUserId(userId);
        pin.setRoleName(roleName);
        repo.save(pin);
        return true;
    }

    public Set<String> pinnedRoleNames(String userId) {
        return repo.findByUserId(userId).stream()
                .map(PinnedMenu::getRoleName)
                .collect(Collectors.toSet());
    }

    /** Pinned items still accessible to the user, in sidebar order. */
    public List<MenuItem> quickMenu(String userId, List<RoleInfo> userRoles) {
        Set<String> pinned = pinnedRoleNames(userId);
        return flattener.flatten(userRoles).stream()
                .filter(item -> pinned.contains(item.name()))
                .toList();
    }
}
