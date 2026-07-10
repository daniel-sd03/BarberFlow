package sodresoftwares.barbearia.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import sodresoftwares.barbearia.model.user.User;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TokenService
 *
 * Tests JWT token generation and validation using the auth0-jwt library
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService Tests")
class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    private User testUser;

    private final String TEST_SECRET = "test-secret-key-for-testing-purposes-only";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "secret", TEST_SECRET);

        testUser = User.builder()
                .id("user-123")
                .login("test@example.com")
                .build();
    }

    @Test
    @DisplayName("Should generate token successfully")
    void shouldGenerateTokenSuccessfully() {
        // Act
        String token = tokenService.generateToken(testUser);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token).contains(".");
    }

    @Test
    @DisplayName("Should validate correct token successfully")
    void shouldValidateCorrectTokenSuccessfully() {
        // Arrange
        String token = tokenService.generateToken(testUser);

        // Act
        String validatedLogin = tokenService.validateToken(token);

        // Assert
        assertThat(validatedLogin).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should return null for invalid token")
    void shouldReturnNullForInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        String result = tokenService.validateToken(invalidToken);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null for tampered token")
    void shouldReturnNullForTamperedToken() {
        // Arrange
        String token = tokenService.generateToken(testUser);
        String tamperedToken = token.substring(0, token.length() - 10) + "0123456789";

        // Act
        String result = tokenService.validateToken(tamperedToken);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null for empty token")
    void shouldReturnNullForEmptyToken() {
        // Act
        String result = tokenService.validateToken("");

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null for null token")
    void shouldReturnNullForNullToken() {
        // Act
        String result = tokenService.validateToken(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should have correct issuer in token")
    void shouldHaveCorrectIssuerInToken() {
        // Arrange
        String token = tokenService.generateToken(testUser);
        Algorithm algorithm = Algorithm.HMAC256(TEST_SECRET);

        // Act & Assert - Verify issuer by decoding without validation
        String issuer = JWT.require(algorithm)
                .withIssuer("auth-api")
                .build()
                .verify(token)
                .getIssuer();

        assertThat(issuer).isEqualTo("auth-api");
    }

    @Test
    @DisplayName("Should have expiration date in the future")
    void shouldHaveExpirationDateInTheFuture() {
        // Arrange
        String token = tokenService.generateToken(testUser);
        Algorithm algorithm = Algorithm.HMAC256(TEST_SECRET);

        // Act
        Instant expiresAt = JWT.require(algorithm)
                .withIssuer("auth-api")
                .build()
                .verify(token)
                .getExpiresAtAsInstant();

        Instant now = Instant.now();

        // Assert
        assertThat(expiresAt).isAfter(now);
    }
}

