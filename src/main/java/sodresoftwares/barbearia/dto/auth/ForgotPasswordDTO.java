package sodresoftwares.barbearia.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordDTO(
        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email format.")
        String email
) {}