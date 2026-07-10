package sodresoftwares.barbearia.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.repositories.UserRepository;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityFilter
 *
 * Tests JWT token extraction and user authentication in HTTP requests.
 * Refactored to eliminate redundant tests and ensure strict context isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityFilter Tests")
class SecurityFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private SecurityFilter securityFilter;

    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-123")
                .login("test@example.com").role(UserRole.USER)
                .build();

        validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.valid-signature";
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate user and set Context when valid token is provided")
    void shouldAuthenticateUserWhenValidTokenIsProvided() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(tokenService.validateToken(validToken)).thenReturn("test@example.com");
        when(userRepository.findByLogin("test@example.com")).thenReturn(testUser);

        // Act
        securityFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(testUser);
        assertThat(authentication.getAuthorities()).isEqualTo(testUser.getAuthorities());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain silently when no token is provided")
    void shouldContinueFilterChainWhenNoTokenIsProvided() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        securityFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(tokenService, never()).validateToken(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate when token is invalid or expired")
    void shouldNotAuthenticateWhenTokenIsInvalid() throws ServletException, IOException {
        // Arrange
        String invalidToken = "invalid-token-here";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(tokenService.validateToken(invalidToken)).thenReturn(null);

        // Act
        securityFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(userRepository, never()).findByLogin(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate when valid token belongs to a deleted user")
    void shouldNotAuthenticateWhenUserIsNotFound() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(tokenService.validateToken(validToken)).thenReturn("deleted@example.com");
        when(userRepository.findByLogin("deleted@example.com")).thenReturn(null);

        // Act
        securityFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
    }
}