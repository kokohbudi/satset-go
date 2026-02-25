# Profile & Self-Service Password Change Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Membangun antarmuka profil pengguna dan fungsionalitas bagi pengguna yang sedang login untuk melihat data diri mereka dan mengubah password mereka sendiri dengan aman, tanpa memerlukan hak akses Admin.

**Architecture:** Hexagonal Architecture (Domain-Driven Design). Controller → UseCase (Domain) → Keycloak Port (Adapter). Menambahkan verifikasi `oldPassword` via Resource Owner Password Credentials (ROPC) grant type ke token endpoint Keycloak sebelum mengganti password.

**Tech Stack:** Spring Boot 4.0.1, Java 25, Keycloak Admin Client, Thymeleaf, Tailwind CSS.

---

### Task 1: Create ChangeMyPasswordRequestDTO

**Files:**
- Create: `src/main/java/com/omnip/identity/adapter/in/web/dto/ChangeMyPasswordRequestDTO.java`

**Step 1: Write the failing test**
(Skipped for simple DTO, as there's no business logic to test yet, just Bean Validation annotations).

**Step 2: Write minimal implementation**

```java
package com.omnip.identity.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeMyPasswordRequestDTO {
    @NotBlank(message = "Password lama wajib diisi")
    private String oldPassword;

    @NotBlank(message = "Password baru wajib diisi")
    @Size(min = 8, message = "Password baru minimal 8 karakter")
    private String newPassword;

    @NotBlank(message = "Konfirmasi password wajib diisi")
    private String confirmPassword;
}
```

**Step 3: Commit**

```bash
git add src/main/java/com/omnip/identity/adapter/in/web/dto/ChangeMyPasswordRequestDTO.java
git commit -m "feat: add ChangeMyPasswordRequestDTO"
```

---

### Task 2: Create Use Case Interface & Update Keycloak Port

**Files:**
- Create: `src/main/java/com/omnip/identity/domain/port/in/ManageMyProfileUseCase.java`
- Modify: `src/main/java/com/omnip/identity/domain/port/out/KeycloakPort.java`

**Step 1: Write minimal implementation**

1. Create `ManageMyProfileUseCase.java`:
```java
package com.omnip.identity.domain.port.in;

import com.omnip.identity.adapter.in.web.dto.ChangeMyPasswordRequestDTO;

public interface ManageMyProfileUseCase {
    void changeMyPassword(String providerUserId, String email, ChangeMyPasswordRequestDTO requestDTO);
}
```

2. Add method to `KeycloakPort.java` (around line 17):
```java
    // ... existing methods ...
    boolean verifyUserPassword(String email, String password);
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/omnip/identity/domain/port/in/ManageMyProfileUseCase.java src/main/java/com/omnip/identity/domain/port/out/KeycloakPort.java
git commit -m "feat: add ManageMyProfileUseCase and KeycloakPort method"
```

---

### Task 3: Implement Keycloak Password Verification (ROPC)

**Files:**
- Modify: `src/main/java/com/omnip/identity/adapter/out/keycloak/KeycloakAdminClientService.java`
- Create/Modify: `src/test/java/com/omnip/identity/adapter/out/keycloak/KeycloakAdminClientServiceTest.java` (Optional/Skip if no easy way to mock RestTemplate)

**Architecture note:** Keycloak Admin Client doesn't have a direct "verify password" method. We must use a standard REST call (e.g., Spring's `RestTemplate` or `RestClient`) to the Keycloak token endpoint using the `password` grant type.

**Step 1: Write minimal implementation**

In `KeycloakAdminClientService.java`:
1. Inject properties for token endpoint and credentials.
2. Implement `verifyUserPassword`.

```java
    // Add to class properties
    @Value("${keycloak.base-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.client-id}")
    private String keycloakClientId;

    @Value("${keycloak.client-secret}")
    private String keycloakClientSecret;

    // ... inside class ...
    @Override
    public boolean verifyUserPassword(String email, String password) {
        String tokenEndpoint = keycloakServerUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";

        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        org.springframework.util.MultiValueMap<String, String> map = new org.springframework.util.LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("client_id", keycloakClientId);
        map.add("client_secret", keycloakClientSecret);
        map.add("username", email);
        map.add("password", password);

        org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> request = new org.springframework.http.HttpEntity<>(map, headers);

        try {
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(tokenEndpoint, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            return false;
        } catch (Exception e) {
            log.error("Error verifying password with Keycloak", e);
            return false;
        }
    }
```

**Step 2: Commit**

```bash
git add src/main/java/com/omnip/identity/adapter/out/keycloak/KeycloakAdminClientService.java
git commit -m "feat: implement Keycloak verifyUserPassword via ROPC"
```

---

### Task 4: Implement Domain Service

**Files:**
- Create: `src/main/java/com/omnip/identity/domain/service/UserSelfServiceDomainService.java`
- Create: `src/test/java/com/omnip/identity/domain/service/UserSelfServiceDomainServiceTest.java`

**Step 1: Write the failing test**

```java
package com.omnip.identity.domain.service;

import com.omnip.identity.adapter.in.web.dto.ChangeMyPasswordRequestDTO;
import com.omnip.identity.domain.port.out.KeycloakPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSelfServiceDomainServiceTest {

    @Mock
    private KeycloakPort keycloakPort;

    private UserSelfServiceDomainService service;

    @BeforeEach
    void setUp() {
        service = new UserSelfServiceDomainService(keycloakPort);
    }

    @Test
    void changeMyPassword_ValidatesPasswordsMatch() {
        ChangeMyPasswordRequestDTO req = new ChangeMyPasswordRequestDTO();
        req.setOldPassword("oldPass");
        req.setNewPassword("newPass123");
        req.setConfirmPassword("mismatchPass");

        assertThrows(IllegalArgumentException.class, () ->
            service.changeMyPassword("user123", "test@test.com", req));
    }

    @Test
    void changeMyPassword_ValidatesOldPassword() {
        ChangeMyPasswordRequestDTO req = new ChangeMyPasswordRequestDTO();
        req.setOldPassword("wrongOld");
        req.setNewPassword("newPass123");
        req.setConfirmPassword("newPass123");

        when(keycloakPort.verifyUserPassword("test@test.com", "wrongOld")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
            service.changeMyPassword("user123", "test@test.com", req));
    }

    @Test
    void changeMyPassword_Success() {
        ChangeMyPasswordRequestDTO req = new ChangeMyPasswordRequestDTO();
        req.setOldPassword("correctOld");
        req.setNewPassword("newPass123");
        req.setConfirmPassword("newPass123");

        when(keycloakPort.verifyUserPassword("test@test.com", "correctOld")).thenReturn(true);

        service.changeMyPassword("user123", "test@test.com", req);

        verify(keycloakPort).changeUserPassword("user123", "newPass123");
    }
}
```

**Step 2: Run test to verify it fails**
Run: `mvn test -Dtest=UserSelfServiceDomainServiceTest`

**Step 3: Write minimal implementation**

```java
package com.omnip.identity.domain.service;

import com.omnip.identity.adapter.in.web.dto.ChangeMyPasswordRequestDTO;
import com.omnip.identity.domain.port.in.ManageMyProfileUseCase;
import com.omnip.identity.domain.port.out.KeycloakPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSelfServiceDomainService implements ManageMyProfileUseCase {

    private final KeycloakPort keycloakPort;

    @Override
    public void changeMyPassword(String providerUserId, String email, ChangeMyPasswordRequestDTO requestDTO) {
        if (!requestDTO.getNewPassword().equals(requestDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Password baru dan konfirmasi tidak cocok");
        }

        if (requestDTO.getOldPassword().equals(requestDTO.getNewPassword())) {
            throw new IllegalArgumentException("Password baru tidak boleh sama dengan password lama");
        }

        boolean isOldPasswordValid = keycloakPort.verifyUserPassword(email, requestDTO.getOldPassword());
        if (!isOldPasswordValid) {
            throw new IllegalArgumentException("Password lama tidak sesuai");
        }

        keycloakPort.changeUserPassword(providerUserId, requestDTO.getNewPassword());
        log.info("User {} successfully changed their password", email);
    }
}
```

**Step 4: Run test to verify it passes**
Run: `mvn test -Dtest=UserSelfServiceDomainServiceTest`

**Step 5: Commit**

```bash
git add src/main/java/com/omnip/identity/domain/service/UserSelfServiceDomainService.java src/test/java/com/omnip/identity/domain/service/UserSelfServiceDomainServiceTest.java
git commit -m "feat: implement UserSelfServiceDomainService"
```

---

### Task 5: Implement UserSelfServiceController (REST API)

**Files:**
- Create: `src/main/java/com/omnip/identity/adapter/in/web/UserSelfServiceController.java`

**Step 1: Write minimal implementation**

```java
package com.omnip.identity.adapter.in.web;

import com.omnip.identity.adapter.in.web.dto.ChangeMyPasswordRequestDTO;
import com.omnip.identity.domain.port.in.ManageMyProfileUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserSelfServiceController {

    private final ManageMyProfileUseCase manageMyProfileUseCase;

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangeMyPasswordRequestDTO requestDTO) {

        String providerUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        try {
            manageMyProfileUseCase.changeMyPassword(providerUserId, email, requestDTO);
            return ResponseEntity.ok(Map.of("message", "Password berhasil diubah"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/omnip/identity/adapter/in/web/UserSelfServiceController.java
git commit -m "feat: add UserSelfServiceController API"
```

---

### Task 6: Implement UserProfileController (Web/Thymeleaf)

**Files:**
- Create: `src/main/java/com/omnip/identity/adapter/in/web/UserProfileController.java`

**Step 1: Write minimal implementation**

```java
package com.omnip.identity.adapter.in.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
public class UserProfileController {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String showProfilePage(@AuthenticationPrincipal Jwt jwt, Model model) {
        model.addAttribute("name", jwt.getClaimAsString("name"));
        model.addAttribute("email", jwt.getClaimAsString("email"));

        // Extract roles from realm_access if needed, or just let Thymeleaf handle it
        return "pages/identity/profile";
    }
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/omnip/identity/adapter/in/web/UserProfileController.java
git commit -m "feat: add UserProfileController"
```

---

### Task 7: Update Header & Create Profile Thymeleaf Page

**Files:**
- Modify: `src/main/resources/templates/components/header.html`
- Create: `src/main/resources/templates/pages/identity/profile.html`

**Step 1: Write minimal implementation**

1. In `header.html`, find the dropdown items:
```html
<a th:href="@{/profile}" class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-600 dark:hover:text-white" role="menuitem">Profil Saya</a>
<a th:href="@{/profile}" class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-600 dark:hover:text-white" role="menuitem">Pengaturan Akun</a>
```

2. Create `profile.html`:
```html
<!DOCTYPE html>
<html lang="id" xmlns:th="http://www.thymeleaf.org"
      th:replace="~{layouts/main :: layout(~{::title}, ~{::main}, null, ~{::script})}">
<head>
    <title>Profil Saya - SatSetGo</title>
</head>
<body>
<main>
    <div class="px-4 py-6 sm:px-0">
        <h1 class="text-2xl font-semibold text-gray-900 dark:text-white mb-6">Profil & Pengaturan Akun</h1>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <!-- Left Column: User Info -->
            <div class="md:col-span-1">
                <div class="bg-white dark:bg-gray-800 shadow rounded-lg p-6">
                    <div class="text-center mb-4">
                        <div class="h-24 w-24 rounded-full bg-blue-500 mx-auto flex items-center justify-center text-white text-3xl font-bold uppercase">
                            <span th:text="${name != null ? #strings.substring(name,0,1) : 'U'}">U</span>
                        </div>
                    </div>
                    <div class="text-center">
                        <h2 class="text-xl font-bold text-gray-900 dark:text-white" th:text="${name}">User Name</h2>
                        <p class="text-sm text-gray-500 dark:text-gray-400" th:text="${email}">user@example.com</p>
                    </div>
                </div>
            </div>

            <!-- Right Column: Password Change Form -->
            <div class="md:col-span-2">
                <div class="bg-white dark:bg-gray-800 shadow rounded-lg p-6">
                    <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-4">Ganti Password</h3>

                    <div id="alertMessage" class="hidden mb-4 p-4 rounded-md"></div>

                    <form id="changePasswordForm">
                        <!-- CSRF Token -->
                        <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" id="csrfToken" />

                        <div class="mb-4">
                            <label for="oldPassword" class="block text-sm font-medium text-gray-700 dark:text-gray-300">Password Lama</label>
                            <input type="password" id="oldPassword" name="oldPassword" required
                                   class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white">
                        </div>

                        <div class="mb-4">
                            <label for="newPassword" class="block text-sm font-medium text-gray-700 dark:text-gray-300">Password Baru</label>
                            <input type="password" id="newPassword" name="newPassword" required minlength="8"
                                   class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white">
                            <p class="mt-1 text-xs text-gray-500">Minimal 8 karakter.</p>
                        </div>

                        <div class="mb-6">
                            <label for="confirmPassword" class="block text-sm font-medium text-gray-700 dark:text-gray-300">Konfirmasi Password Baru</label>
                            <input type="password" id="confirmPassword" name="confirmPassword" required minlength="8"
                                   class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white">
                        </div>

                        <div>
                            <button type="submit" id="submitBtn"
                                    class="inline-flex justify-center rounded-md border border-transparent bg-blue-600 py-2 px-4 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2">
                                Simpan Password Baru
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</main>

<script>
    document.getElementById('changePasswordForm').addEventListener('submit', function(e) {
        e.preventDefault();

        const submitBtn = document.getElementById('submitBtn');
        const alertBox = document.getElementById('alertMessage');
        const csrfToken = document.getElementById('csrfToken').value;

        const oldPassword = document.getElementById('oldPassword').value;
        const newPassword = document.getElementById('newPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        // Basic validation
        if (newPassword !== confirmPassword) {
            showAlert('error', 'Password baru dan konfirmasi tidak cocok!');
            return;
        }

        // UI Loading state
        submitBtn.disabled = true;
        submitBtn.innerText = 'Menyimpan...';
        alertBox.classList.add('hidden');

        fetch('/api/users/me/password', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': csrfToken
            },
            body: JSON.stringify({
                oldPassword: oldPassword,
                newPassword: newPassword,
                confirmPassword: confirmPassword
            })
        })
        .then(response => response.json().then(data => ({status: response.status, body: data})))
        .then(res => {
            if (res.status === 200) {
                showAlert('success', res.body.message || 'Password berhasil diubah!');
                document.getElementById('changePasswordForm').reset();
            } else {
                showAlert('error', res.body.error || 'Terjadi kesalahan saat mengubah password.');
            }
        })
        .catch(err => {
            showAlert('error', 'Gagal terhubung ke server.');
        })
        .finally(() => {
            submitBtn.disabled = false;
            submitBtn.innerText = 'Simpan Password Baru';
        });
    });

    function showAlert(type, message) {
        const alertBox = document.getElementById('alertMessage');
        alertBox.classList.remove('hidden', 'bg-red-50', 'text-red-800', 'bg-green-50', 'text-green-800');

        if (type === 'error') {
            alertBox.classList.add('bg-red-50', 'text-red-800');
        } else {
            alertBox.classList.add('bg-green-50', 'text-green-800');
        }

        alertBox.innerText = message;
    }
</script>
</body>
</html>
```

**Step 2: Commit**

```bash
git add src/main/resources/templates/components/header.html src/main/resources/templates/pages/identity/profile.html
git commit -m "feat: add profile UI and connect to change password API"
```

---

### Task 8: Run End-to-End Build and Verification

**Step 1: Build project**
Run: `mvn clean package -DskipTests=true`

**Step 2: Start application (Optional/Manual)**
Run: `mvn spring-boot:run`
Manually verify the UI and Keycloak integration.

**Step 3: Update Tasks.md**
Update `Tasks.md` to move "Self-Service Password Change" to DONE/COMPLETED.

```bash
git add Tasks.md
git commit -m "chore: update Tasks.md status"
```