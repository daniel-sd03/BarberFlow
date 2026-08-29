package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.auth.TokenRefreshResponseDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.infra.security.TokenService;
import sodresoftwares.barbearia.model.RefreshToken;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.repositories.RefreshTokenRepository;
import sodresoftwares.barbearia.repositories.UserRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${app.security.jwt.refresh-expiration:2592000000}")
    private Long refreshTokenDurationMs;

    @Value("${app.lgpd.current-version}")
    private String currentLgpdVersion;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Transactional
    public RefreshToken createOrReuseRefreshToken(User user) {

        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserId(user.getId());

        if (existingToken.isPresent()) {
            RefreshToken token = existingToken.get();

            // If the token is still valid, extend its expiration date and reuse it
            if (token.getExpiryDate().compareTo(Instant.now()) > 0) {
                token.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
                return refreshTokenRepository.save(token);
            } else {
                // If it's expired, delete the old one to make room for a new one
                refreshTokenRepository.delete(token);
            }
        }

        String uniqueToken;
        do {
            uniqueToken = UUID.randomUUID().toString();
        } while (refreshTokenRepository.existsByToken(uniqueToken));

        // Create a brand-new refresh token
        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .token(uniqueToken)
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        return refreshTokenRepository.save(newToken);
    }

    @Transactional
    public TokenRefreshResponseDTO processRefreshToken(String requestRefreshToken) {
        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String newAccessToken = tokenService.generateToken(user, currentLgpdVersion);
                    return new TokenRefreshResponseDTO(newAccessToken, requestRefreshToken);
                })
                .orElseThrow(() -> new AppException(
                        HttpStatus.FORBIDDEN,
                        "INVALID_REFRESH_TOKEN",
                        "Refresh token is invalid or not found."
                ));
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new AppException(
                    HttpStatus.UNAUTHORIZED,
                    "REFRESH_TOKEN_EXPIRED",
                    "The session has completely expired. Please log in again."
            );
        }
        return token;
    }
}