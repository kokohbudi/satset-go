package com.omnip.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for creating a new user.
 * Contains validation constraints for all required fields.
 */
@Data
public class CreateUserRequest {

    @NotBlank(message = "Username wajib diisi")
    @Size(min = 3, max = 50, message = "Username harus antara 3-50 karakter")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username hanya boleh huruf, angka, dan underscore")
    private String username;

    @NotBlank(message = "Email wajib diisi")
    @Email(message = "Format email tidak valid")
    private String email;

    @NotBlank(message = "Nama lengkap wajib diisi")
    @Size(min = 2, max = 100, message = "Nama lengkap harus antara 2-100 karakter")
    private String fullname;

    @NotBlank(message = "Password wajib diisi")
    @Size(min = 6, max = 100, message = "Password minimal 6 karakter")
    private String password;

    /**
     * List of role names to assign to the user.
     * Can be empty if no roles should be assigned.
     */
    private List<String> roles;

    /**
     * Whether the user must change password on first login.
     */
    private boolean requirePasswordChange = true;
}
