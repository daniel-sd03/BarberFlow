package sodresoftwares.barbearia.dto.auth;

import jakarta.validation.constraints.*;

public record RegisterDTO(
        @NotBlank(message = "Login is required")
        @Email(message = "Invalid email format")
        String login,

        @NotBlank(message = "Password is required")
        String password,

        @NotBlank(message = "name is required")
        String name,

        @Pattern(regexp = "^\\d{10,11}$", message = "Phone must contain 10 or 11 digits")
        String phone,

        @NotNull(message = "Terms acceptance is required")
        @AssertTrue(message = "You must accept the terms of use and privacy policy to register")
        Boolean termsAccepted
) {}
