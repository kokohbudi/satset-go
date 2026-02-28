package com.omnip.identity.adapter.in.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for the "change my password" request.
 *
 * <p>Carries the desired new password ({@code newPassword}) together with a confirmation copy
 * ({@code confirmPassword}). A class-level cross-field validation ensures that
 * {@code newPassword} and {@code confirmPassword} are identical before the request
 * is processed by the domain service.
 */
@Data
public class ChangeMyPasswordRequestDTO {

    @NotBlank(message = "Password baru wajib diisi")
    @Size(min = 8, max = 128, message = "Password baru harus antara 8 sampai 128 karakter")
    private String newPassword;

    @NotBlank(message = "Konfirmasi password wajib diisi")
    @Size(max = 128, message = "Konfirmasi password maksimal 128 karakter")
    private String confirmPassword;

    /**
     * Cross-field validation: returns {@code true} when {@code newPassword} and
     * {@code confirmPassword} are both non-null and equal to each other.
     *
     * @return {@code true} if the two password fields match
     */
    @AssertTrue(message = "Password baru dan konfirmasi password harus sama")
    public boolean isPasswordsMatching() {
        if (newPassword == null || confirmPassword == null) {
            return false;
        }
        return newPassword.equals(confirmPassword);
    }
}
