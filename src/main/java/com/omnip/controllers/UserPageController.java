package com.omnip.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for User Management pages (Thymeleaf frontend).
 * Handles the web UI routes for user CRUD operations.
 */
@Controller
@Slf4j
public class UserPageController {

    @GetMapping("/users")
    public String usersPage(Model model) {
        log.info("Accessing users list page");
        model.addAttribute("currentPage", "users");
        model.addAttribute("breadcrumb", "Daftar User");
        // TODO: Fetch users from service and add to model
        // model.addAttribute("users", userService.getAllUsers());
        return "pages/users/index";
    }

    @GetMapping("/groups")
    public String groupsPage(Model model) {
        log.info("Accessing groups management page");
        model.addAttribute("currentPage", "groups");
        model.addAttribute("breadcrumb", "Group Management");
        // TODO: Fetch groups from service and add to model
        // model.addAttribute("groups", groupService.getAllGroups());
        return "pages/groups/index";
    }

    @GetMapping("/user-groups")
    public String userGroupsPage(Model model) {
        log.info("Accessing user-groups assignment page");
        model.addAttribute("currentPage", "user-groups");
        model.addAttribute("breadcrumb", "Assign User ke Group");
        // TODO: Fetch available users and groups from service
        // model.addAttribute("availableUsers", userService.getAvailableUsers());
        // model.addAttribute("groups", groupService.getAllGroups());
        return "pages/user-groups/index";
    }
}
