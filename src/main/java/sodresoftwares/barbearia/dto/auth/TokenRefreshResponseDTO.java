package sodresoftwares.barbearia.dto.auth;

public record TokenRefreshResponseDTO(
        String accessToken,
        String refreshToken
) {}