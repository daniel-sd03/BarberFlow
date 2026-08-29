package sodresoftwares.barbearia.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequestDTO(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}
