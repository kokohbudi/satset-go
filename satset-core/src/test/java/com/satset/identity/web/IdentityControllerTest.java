package com.satset.identity.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.satset.identity.service.IdentityDomainService;
import com.satset.shared.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IdentityControllerTest {

    @Mock private IdentityDomainService identityService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        IdentityController controller = new IdentityController(identityService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ==================== getBackofficeUsers — controller-level filter logic ====================

    @Test
    void getBackofficeUsers_NoQueryParam_ReturnsAllUsers() throws Exception {
        List<UserDTO> users = List.of(buildUser("alice@mail.com", "Alice", "alice"),
                buildUser("bob@mail.com", "Bob", "bob"));
        when(identityService.getBackofficeUsers(any())).thenReturn(users);

        mockMvc.perform(get("/api/idm/backoffice/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getBackofficeUsers_WithQuery_FiltersByFullname() throws Exception {
        List<UserDTO> users = List.of(
                buildUser("alice@mail.com", "Alice Smith", "asmith"),
                buildUser("bob@mail.com", "Bob Jones", "bjones"));
        when(identityService.getBackofficeUsers(any())).thenReturn(users);

        mockMvc.perform(get("/api/idm/backoffice/users").param("q", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fullname").value("Alice Smith"));
    }

    @Test
    void getBackofficeUsers_WithQuery_FiltersByEmail() throws Exception {
        List<UserDTO> users = List.of(
                buildUser("alice@mail.com", "Alice", "alice"),
                buildUser("bob@company.com", "Bob", "bob"));
        when(identityService.getBackofficeUsers(any())).thenReturn(users);

        mockMvc.perform(get("/api/idm/backoffice/users").param("q", "company"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("bob@company.com"));
    }

    @Test
    void getBackofficeUsers_WithQuery_FiltersByUsername() throws Exception {
        List<UserDTO> users = List.of(
                buildUser("alice@mail.com", "Alice", "alice_admin"),
                buildUser("bob@mail.com", "Bob", "bob_user"));
        when(identityService.getBackofficeUsers(any())).thenReturn(users);

        mockMvc.perform(get("/api/idm/backoffice/users").param("q", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("alice_admin"));
    }

    @Test
    void getBackofficeUsers_WithQuery_CaseInsensitive() throws Exception {
        List<UserDTO> users = List.of(buildUser("ALICE@MAIL.COM", "ALICE SMITH", "ALICE"));
        when(identityService.getBackofficeUsers(any())).thenReturn(users);

        mockMvc.perform(get("/api/idm/backoffice/users").param("q", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getBackofficeUsers_WithQuery_NoMatch_ReturnsEmpty() throws Exception {
        List<UserDTO> users = List.of(buildUser("alice@mail.com", "Alice", "alice"));
        when(identityService.getBackofficeUsers(any())).thenReturn(users);

        mockMvc.perform(get("/api/idm/backoffice/users").param("q", "zzz-no-match"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getBackofficeUsers_WithBlankQuery_ReturnsAll() throws Exception {
        List<UserDTO> users = List.of(buildUser("alice@mail.com", "Alice", "alice"),
                buildUser("bob@mail.com", "Bob", "bob"));
        when(identityService.getBackofficeUsers(any())).thenReturn(users);

        mockMvc.perform(get("/api/idm/backoffice/users").param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ==================== createBackofficeUser ====================

    @Test
    void createBackofficeUser_MapsRequestFieldsToUserDTO() throws Exception {
        UserDTO returned = buildUser("new@mail.com", "New User", "newuser");
        when(identityService.createBackofficeUser(any())).thenReturn(returned);

        String body = """
                {
                    "username": "newuser",
                    "email": "new@mail.com",
                    "fullname": "New User",
                    "password": "secret123",
                    "roles": ["manage_users"]
                }
                """;

        mockMvc.perform(post("/api/idm/backoffice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@mail.com"));

        verify(identityService).createBackofficeUser(any(UserDTO.class));
    }

    // ==================== Groups ====================

    @Test
    void getGroups_ReturnsOk() throws Exception {
        when(identityService.getGroups()).thenReturn(List.of());

        mockMvc.perform(get("/api/idm/groups"))
                .andExpect(status().isOk());
    }

    @Test
    void getGroupsHierarchy_ReturnsOk() throws Exception {
        when(identityService.getGroupsHierarchy()).thenReturn(List.of());

        mockMvc.perform(get("/api/idm/groups/hierarchy"))
                .andExpect(status().isOk());
    }

    @Test
    void getSubGroups_ReturnsOk() throws Exception {
        when(identityService.getSubGroups("/backoffice")).thenReturn(List.of());

        mockMvc.perform(get("/api/idm/groups/subgroups").param("parentPath", "/backoffice"))
                .andExpect(status().isOk());
    }

    @Test
    void getGroupMembers_ReturnsOk() throws Exception {
        when(identityService.getGroupMembers("g-1", false)).thenReturn(List.of());

        mockMvc.perform(get("/api/idm/groups/g-1/members"))
                .andExpect(status().isOk());
    }

    @Test
    void getGroupMembers_Recursive_PassesFlagToUseCase() throws Exception {
        when(identityService.getGroupMembers("g-1", true)).thenReturn(List.of());

        mockMvc.perform(get("/api/idm/groups/g-1/members").param("recursive", "true"))
                .andExpect(status().isOk());

        verify(identityService).getGroupMembers("g-1", true);
    }

    @Test
    void getUserGroups_ReturnsOk() throws Exception {
        when(identityService.getUserGroups("u-1")).thenReturn(List.of());

        mockMvc.perform(get("/api/idm/users/u-1/groups"))
                .andExpect(status().isOk());
    }

    @Test
    void assignUserToGroup_ReturnsOk() throws Exception {
        when(identityService.assignUserToGroup("u-1", "g-1"))
                .thenReturn(Map.of("status", "success"));

        mockMvc.perform(post("/api/idm/users/u-1/groups/g-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void removeUserFromGroup_ReturnsOk() throws Exception {
        when(identityService.removeUserFromGroup("u-1", "g-1"))
                .thenReturn(Map.of("status", "success"));

        mockMvc.perform(delete("/api/idm/users/u-1/groups/g-1"))
                .andExpect(status().isOk());
    }

    // ==================== Roles ====================

    @Test
    void getClientRoles_ReturnsOk() throws Exception {
        when(identityService.getRoles()).thenReturn(List.of());

        mockMvc.perform(get("/api/idm/roles"))
                .andExpect(status().isOk());
    }

    @Test
    void getRolesByScope_ReturnsOk() throws Exception {
        when(identityService.getRolesByScope("backoffice")).thenReturn(List.of());

        mockMvc.perform(get("/api/idm/roles/scope/backoffice"))
                .andExpect(status().isOk());
    }

    @Test
    void getRolesByGroup_ReturnsOk() throws Exception {
        when(identityService.getRolesByGroup("g-1")).thenReturn(List.of());

        mockMvc.perform(get("/api/idm/groups/g-1/roles"))
                .andExpect(status().isOk());
    }

    @Test
    void assignRoleToGroup_ReturnsOk() throws Exception {
        when(identityService.assignRoleToGroup("g-1", "manage_users"))
                .thenReturn(Map.of("status", "success"));

        mockMvc.perform(post("/api/idm/groups/g-1/roles/manage_users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void unassignRoleFromGroup_ReturnsOk() throws Exception {
        when(identityService.unassignRoleFromGroup("g-1", "manage_users"))
                .thenReturn(Map.of("status", "success"));

        mockMvc.perform(delete("/api/idm/groups/g-1/roles/manage_users"))
                .andExpect(status().isOk());
    }

    @Test
    void assignRoleToUser_ReturnsOk() throws Exception {
        doNothing().when(identityService).assignRoleToUser("u-1", "manage_users");

        mockMvc.perform(post("/api/idm/users/u-1/roles/manage_users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void unassignRoleFromUser_ReturnsOk() throws Exception {
        doNothing().when(identityService).unassignRoleFromUser("u-1", "manage_users");

        mockMvc.perform(delete("/api/idm/users/u-1/roles/manage_users"))
                .andExpect(status().isOk());
    }

    // ==================== User management ====================

    @Test
    void getAllUsers_ReturnsOk() throws Exception {
        when(identityService.getAllUsers(100)).thenReturn(List.of());

        mockMvc.perform(get("/api/idm/users"))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_ReturnsOk() throws Exception {
        UserDTO result = new UserDTO();
        result.setStatus("success");
        when(identityService.changePassword(any())).thenReturn(result);

        mockMvc.perform(put("/api/idm/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@mail.com\",\"password\":\"newpass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void setUserStatus_ReturnsOk() throws Exception {
        UserDTO result = new UserDTO();
        result.setStatus("success");
        when(identityService.setUserStatus("alice@mail.com", false)).thenReturn(result);

        mockMvc.perform(put("/api/idm/users/alice@mail.com/status/false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void setBackofficeUserStatus_ReturnsOk() throws Exception {
        UserDTO result = new UserDTO();
        result.setStatus("success");
        when(identityService.setBackofficeUserStatus("kc-uuid", true)).thenReturn(result);

        mockMvc.perform(put("/api/idm/backoffice/users/kc-uuid/status/true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    // ==================== Helpers ====================

    private UserDTO buildUser(String email, String fullname, String username) {
        UserDTO dto = new UserDTO();
        dto.setEmail(email);
        dto.setFullname(fullname);
        dto.setUsername(username);
        return dto;
    }
}
