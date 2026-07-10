package sodresoftwares.barbearia.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationDTO(
        @NotBlank(message = "barbearia is required")
        String login,

        @NotBlank(message = "Password is required")
        String password
) {}