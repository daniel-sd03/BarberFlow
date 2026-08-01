package sodresoftwares.barbearia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDTO(
        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email format.")
        String email,

        @NotBlank(message = "Code is required.")
        @Size(min = 6, max = 6, message = "Code must be exactly 6 digits.")
        String code,

        @NotBlank(message = "New password is required.")
        @Size(min = 6, max = 100, message = "New password must be between 6 and 100 characters.")
        String newPassword,

        @NotBlank(message = "Password confirmation is required.")
        String confirmPassword
) {}