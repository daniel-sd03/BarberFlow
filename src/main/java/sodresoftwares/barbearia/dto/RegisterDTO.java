package sodresoftwares.barbearia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import sodresoftwares.barbearia.model.user.UserRole;

public record RegisterDTO(
        @NotBlank(message = "Login is required")
        @Email(message = "Invalid email format")
        String login,

        @NotBlank(message = "Password is required")
        String password,

        @NotBlank(message = "name is required")
        String name,

        @Pattern(regexp = "^\\d{10,11}$", message = "Phone must contain 10 or 11 digits")
        String phone
) {}
