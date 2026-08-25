package sodresoftwares.barbearia.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ValidateTokenDTO(
        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email format.")
        String email,

        @NotBlank(message = "Code is required.")
        @Size(min = 6, max = 6, message = "Code must be exactly 6 digits.")
        String code
) {}