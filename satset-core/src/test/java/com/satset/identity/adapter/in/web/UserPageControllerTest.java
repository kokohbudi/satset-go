package com.satset.identity.adapter.in.web;

import com.satset.identity.domain.port.in.ManageBackofficeUsersUseCase;
import com.satset.identity.domain.port.in.ManageGroupsUseCase;
import com.satset.identity.domain.port.in.ManageRolesUseCase;
import com.satset.shared.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserPageControllerTest {

    @Mock private ManageBackofficeUsersUseCase manageBackofficeUsersUseCase;
    @Mock private ManageGroupsUseCase manageGroupsUseCase;
    @Mock private ManageRolesUseCase manageRolesUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new UserPageController(manageBackofficeUsersUseCase,
                        manageGroupsUseCase, manageRolesUseCase)).build();
    }

    @Test
    void userManagementPage_ReturnsViewAndAttributes() throws Exception {
        when(manageBackofficeUsersUseCase.getBackofficeUsers()).thenReturn(List.of());
        when(manageGroupsUseCase.getBackofficeSubGroups()).thenReturn(List.of());
        when(manageRolesUseCase.getRolesForDropdown()).thenReturn(List.of());

        mockMvc.perform(get("/admin/user-management"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/user-management"))
                .andExpect(model().attribute("currentPage", "user-management"))
                .andExpect(model().attribute("breadcrumb", "User Management"));
    }

    @Test
    void userManagementPage_MapsUsersToViewModel() throws Exception {
        UserDTO user = new UserDTO();
        user.setEmail("alice@mail.com");
        user.setFullname("Alice");
        when(manageBackofficeUsersUseCase.getBackofficeUsers()).thenReturn(List.of(user));
        when(manageGroupsUseCase.getBackofficeSubGroups()).thenReturn(List.of());
        when(manageRolesUseCase.getRolesForDropdown()).thenReturn(List.of());

        mockMvc.perform(get("/admin/user-management"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("initialUsers"));
    }
}
