package com.omnip.configs;

import com.omnip.dtos.UserDTO;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Implementasi interface AuditorAware untuk mendapatkan auditor saat ini.
 * Kelas ini digunakan oleh Spring Data JPA Auditing untuk secara otomatis
 * mengisi field seperti createdBy dan updatedBy.
 */
public class AuditorAwareImpl implements AuditorAware<String> {

    private final UserDTO userDTO;

    /**
     * Konstruktor dengan dependency injection UserDTO.
     *
     * @param userDTO Objek DTO yang berisi informasi pengguna saat ini
     */
    public AuditorAwareImpl(UserDTO userDTO) {
        this.userDTO = userDTO;
    }

    /**
     * Mendapatkan nama auditor saat ini dari konteks keamanan.
     * Method ini mencoba mendapatkan nama pengguna dari:
     * 1. UserDTO jika tersedia (diutamakan)
     * 2. Nama dari objek Authentication
     * 3. "SYSTEM" jika tidak ada autentikasi
     *
     * @return Optional<String> berisi nama auditor atau "SYSTEM" jika tidak terautentikasi
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("SYSTEM");
        }

        // Jika menggunakan OAuth2/JWT
        if (this.userDTO != null) {
            return Optional.of(this.userDTO.getUsername());
        }

        return Optional.of(authentication.getName());
    }
}