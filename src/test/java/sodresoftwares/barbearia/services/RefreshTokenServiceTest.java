package sodresoftwares.barbearia.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import sodresoftwares.barbearia.dto.auth.TokenRefreshResponseDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.infra.security.TokenService;
import sodresoftwares.barbearia.model.RefreshToken;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.repositories.RefreshTokenRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 2592000000L);
        ReflectionTestUtils.setField(refreshTokenService, "currentLgpdVersion", "1.0");

        testUser = User.builder().id("user-123").login("test@mail.com").build();
    }

    // ==================== CREAT OR REUSE REFRESH TOKEN TESTS  ====================

    @Test
    @DisplayName("Should create a brand new refresh token when user has none")
    void testCreateOrReuse_NewToken() {
        when(refreshTokenRepository.findByUserId("user-123")).thenReturn(Optional.empty());
        when(refreshTokenRepository.existsByToken(anyString())).thenReturn(false);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        RefreshToken result = refreshTokenService.createOrReuseRefreshToken(testUser);

        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertNotNull(result.getToken());
        assertTrue(result.getExpiryDate().isAfter(Instant.now()));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should reuse existing token if it is still valid")
    void testCreateOrReuse_ReuseValidToken() {
        RefreshToken validToken = RefreshToken.builder()
                .token("existing-token")
                .expiryDate(Instant.now().plusMillis(100000))
                .build();

        when(refreshTokenRepository.findByUserId("user-123")).thenReturn(Optional.of(validToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(validToken);

        RefreshToken result = refreshTokenService.createOrReuseRefreshToken(testUser);

        assertEquals("existing-token", result.getToken());
        verify(refreshTokenRepository, never()).delete(any());
    }

    // ==================== PROCESS REFRESH TOKEN TESTS  ====================

    @Test
    @DisplayName("Should process a valid refresh token and generate a new access token")
    void testProcessRefreshToken_Success() {
        RefreshToken validToken = RefreshToken.builder()
                .token("valid-token")
                .user(testUser)
                .expiryDate(Instant.now().plusMillis(100000))
                .build();

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(validToken));
        when(tokenService.generateToken(testUser, "1.0")).thenReturn("new-jwt-access-token");

        TokenRefreshResponseDTO response = refreshTokenService.processRefreshToken("valid-token");

        assertEquals("new-jwt-access-token", response.accessToken());
        assertEquals("valid-token", response.refreshToken());
    }

    @Test
    @DisplayName("Should throw AppException when refresh token is expired")
    void testVerifyExpiration_ThrowsException() {
        RefreshToken expiredToken = RefreshToken.builder()
                .token("expired-token")
                .expiryDate(Instant.now().minusMillis(100000))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        AppException exception = assertThrows(AppException.class, () ->
                refreshTokenService.processRefreshToken("expired-token"));

        assertEquals("REFRESH_TOKEN_EXPIRED", exception.getErrorCode());
        verify(refreshTokenRepository).delete(expiredToken);
    }
}