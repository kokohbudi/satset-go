package com.satset.quickmenu.service.menu;

import com.satset.quickmenu.model.MenuItem;
import com.satset.shared.dto.RoleInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Flattens nested role info into the ordered list of sidebar menu items,
 * matching the visibility rules in templates/components/sidebar.html:
 * an item is shown when attribute sidebar == "1" and url is non-blank;
 * children of a composite role are items, the parent is only a header.
 */
@Component
public class MenuFlattener {

    public List<MenuItem> flatten(List<RoleInfo> roles) {
        List<MenuItem> out = new ArrayList<>();
        if (roles == null) {
            return out;
        }
        for (RoleInfo role : roles) {
            List<RoleInfo> children = role.getChildren();
            if (children != null && !children.isEmpty()) {
                for (RoleInfo child : children) {
                    if (isMenu(child)) {
                        out.add(toItem(child));
                    }
                }
            } else if (isMenu(role)) {
                out.add(toItem(role));
            }
        }
        return out;
    }

    private boolean isMenu(RoleInfo r) {
        Map<String, String> a = r.getAttributes();
        return a != null
                && "1".equals(a.get("sidebar"))
                && a.get("url") != null
                && !a.get("url").isBlank();
    }

    private MenuItem toItem(RoleInfo r) {
        Map<String, String> a = r.getAttributes();
        String icon = a.get("icon");
        if (icon == null || icon.isBlank()) {
            icon = "icon-document";
        }
        String displayName = a.get("display_name");
        if (displayName == null || displayName.isBlank()) {
            displayName = r.getName();
        }
        return new MenuItem(r.getName(), a.get("url"), icon, displayName);
    }
}
