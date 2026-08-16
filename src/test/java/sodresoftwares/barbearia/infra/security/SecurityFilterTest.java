package sodresoftwares.barbearia.infra.security;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityFilter Tests")
class SecurityFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;

    @Mock
    private DecodedJWT decodedJWT;

    @InjectMocks
    private SecurityFilter securityFilter;

    private User testUser;
    private String validToken;
    private final String CURRENT_LGPD_VERSION = "2.0";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(securityFilter, "currentLgpdVersion", CURRENT_LGPD_VERSION);
        validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.valid-signature";
        testUser = User.builder().id("user-123").login("test@example.com").role(UserRole.USER).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockJwtLgpdClaim(String lgpdVersion) {
        when(tokenService.validateAndDecodeToken(validToken)).thenReturn(decodedJWT);
        Claim lgpdClaim = mock(Claim.class);
        when(decodedJWT.getClaim("lgpd_version")).thenReturn(lgpdClaim);
        when(lgpdClaim.asString()).thenReturn(lgpdVersion);
    }

    @Test
    @DisplayName("Should authenticate user and set Context when valid token is provided")
    void shouldAuthenticateUserWhenValidTokenIsProvided() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(request.getRequestURI()).thenReturn("/alguma-rota");

        mockJwtLgpdClaim(CURRENT_LGPD_VERSION);

        var auth = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        when(tokenService.getAuthentication(decodedJWT)).thenReturn(auth);

        securityFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(testUser);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain silently when no token is provided")
    void shouldContinueFilterChainWhenNoTokenIsProvided() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        securityFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should NOT authenticate and throw exception when LGPD terms are outdated")
    void shouldNotAuthenticateWhenLgpdIsOutdated() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(request.getRequestURI()).thenReturn("/professionals/me");

        mockJwtLgpdClaim("1.0");

        securityFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(handlerExceptionResolver).resolveException(eq(request), eq(response), isNull(), any(AppException.class));
        verify(filterChain, never()).doFilter(any(), any());
    }
}