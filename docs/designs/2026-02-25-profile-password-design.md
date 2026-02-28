# Technical Design: Profile & Self-Service Password Change

**Date**: 2026-02-25
**Status**: Approved
**Author**: Julia (AI Assistant)
**Context**: SatSetGo (omnip-services-3) - Technical Debt Resolution

## 1. Context & Scope
Membangun antarmuka profil pengguna dan fungsionalitas bagi pengguna yang sedang login untuk melihat data diri mereka dan mengubah password mereka sendiri dengan aman, tanpa memerlukan hak akses Admin. Pengguna tidak perlu keluar dari aplikasi (Custom UI).

## 2. Arsitektur & Komponen (Hexagonal Architecture)

*   **Web Controller (UI):** `UserProfileController`
    *   `GET /profile`: Mengembalikan halaman Thymeleaf `profile.html`. Data pengguna diambil dari token JWT / SecurityContext atau database lokal.
*   **REST API Controller:** `UserSelfServiceController`
    *   `PUT /api/users/me/password`: Endpoint untuk menerima *request* ganti password.
*   **Domain Use Case & Service:** `ManageMyProfileUseCase` & `UserSelfServiceDomainService`
    *   Validasi bahwa `newPassword` dan `confirmPassword` sama.
    *   Validasi `oldPassword` dengan mencoba melakukan autentikasi (Direct Access Grant / Resource Owner Password Credentials) ke Keycloak menggunakan email pengguna dan `oldPassword`.
    *   Jika valid, panggil `KeycloakAdminClientService.changeUserPassword()` menggunakan ID pengguna yang sedang login (`providerUserId`).
*   **Keamanan (Security):**
    *   Endpoint menggunakan ID dari `SecurityContextHolder` (pengguna yang sedang login), bukan dari parameter URL/Body, untuk mencegah manipulasi ID (Insecure Direct Object Reference).
    *   Endpoint tidak memerlukan role `REALM_manage_users`, cukup terautentikasi (semua role).

## 3. Data Model & DTO

**ChangeMyPasswordRequestDTO**
```java
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

## 4. UI/UX Flow (Thymeleaf + Tailwind)

1.  **Update `header.html`**:
    *   Mengaktifkan link `/profile` dan mengubah href untuk `/settings/account` agar mengarah ke `/profile` juga.
2.  **Pembuatan `profile.html`**:
    *   **Layout Utama**: Dibagi menjadi dua bagian (kiri dan kanan).
    *   **Card Kiri (Informasi Pengguna)**: Menampilkan info statis (Nama Lengkap, Email, Role, Status) yang diambil dari Principal/JWT.
    *   **Card Kanan (Ganti Password)**: Form dengan 3 input (Password Lama, Password Baru, Konfirmasi Password Baru).
    *   **Interaksi**: Form di-submit menggunakan JavaScript (Fetch API/AJAX) agar transisinya mulus. Menampilkan notifikasi sukses/gagal (Toast/Alert) tanpa me-reload halaman.

## 5. Testing Strategy

*   **Unit Test (Domain/Service)**:
    *   Memastikan exception dilempar jika `newPassword` tidak sama dengan `confirmPassword`.
    *   Memastikan pemanggilan ke Keycloak adapter menggunakan ID yang benar dari context.
    *   Memastikan flow gagal jika verifikasi `oldPassword` gagal.
*   **Security Test (Controller)**:
    *   Endpoint `/api/users/me/password` hanya bisa diakses oleh user yang terautentikasi.
    *   User A tidak bisa mengganti password User B.
