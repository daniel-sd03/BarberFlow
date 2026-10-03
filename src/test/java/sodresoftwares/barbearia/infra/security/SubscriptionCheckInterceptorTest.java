package sodresoftwares.barbearia.infra.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import sodresoftwares.barbearia.model.billing.SubscriptionStatus;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.repositories.billing.SubscriptionRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionCheckInterceptor Tests")
class SubscriptionCheckInterceptorTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private SubscriptionCheckInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private User testUser;
    private final String USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        testUser = User.builder()
                .id(USER_ID)
                .login("barbeiro@teste.com")
                .build();
    }

    @AfterEach
    void tearDown() {
        // Clear the security context after each test to prevent state leakage to other tests
        SecurityContextHolder.clearContext();
    }

    // ==================== PUBLIC ROUTES AND SECURITY ====================

    @Test
    @DisplayName("Should allow request when authentication is null")
    void shouldAllowWhenAuthenticationIsNull() throws Exception {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertThat(result).isTrue();
        verify(subscriptionRepository, never()).findSubscriptionStatusByUserId(any());
    }

    @Test
    @DisplayName("Should allow request when user is not authenticated")
    void shouldAllowWhenNotAuthenticated() throws Exception {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should allow request when principal is anonymousUser")
    void shouldAllowWhenAnonymousUser() throws Exception {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should log error and allow request when principal is not a User object (Security Anomaly)")
    void shouldAllowAndLogWhenPrincipalIsNotUser() throws Exception {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("SomeStringPrincipal");

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertThat(result).isTrue();
        verify(subscriptionRepository, never()).findSubscriptionStatusByUserId(any());
    }

    // ==================== SUBSCRIPTION VALIDATIONS ====================

    @Test
    @DisplayName("Should allow request when subscription is ACTIVE")
    void shouldAllowWhenSubscriptionIsActive() throws Exception {
        // Arrange
        setupAuthenticatedUser();
        when(subscriptionRepository.findSubscriptionStatusByUserId(USER_ID))
                .thenReturn(Optional.of(SubscriptionStatus.ACTIVE));

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should allow request when subscription is TRIAL")
    void shouldAllowWhenSubscriptionIsTrial() throws Exception {
        // Arrange
        setupAuthenticatedUser();
        when(subscriptionRepository.findSubscriptionStatusByUserId(USER_ID))
                .thenReturn(Optional.of(SubscriptionStatus.TRIAL));

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should allow request when subscription is not found (null status)")
    void shouldAllowWhenSubscriptionNotFound() throws Exception {
        // Arrange
        setupAuthenticatedUser();
        when(subscriptionRepository.findSubscriptionStatusByUserId(USER_ID))
                .thenReturn(Optional.empty());

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should block request with 402 PAYMENT_REQUIRED when subscription is SUSPENDED")
    void shouldBlockWhenSubscriptionIsSuspended() throws Exception {
        // Arrange
        setupAuthenticatedUser();
        when(subscriptionRepository.findSubscriptionStatusByUserId(USER_ID))
                .thenReturn(Optional.of(SubscriptionStatus.SUSPENDED));

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.PAYMENT_REQUIRED.value());
        assertThat(response.getContentType()).isEqualTo("application/json");

        // Check the JSON written in the response
        String jsonResponse = response.getContentAsString();
        assertThat(jsonResponse).contains("PAYMENT_REQUIRED");
        assertThat(jsonResponse).contains("Your subscription is suspended or canceled");
    }

    @Test
    @DisplayName("Should block request with 402 PAYMENT_REQUIRED when subscription is CANCELED")
    void shouldBlockWhenSubscriptionIsCanceled() throws Exception {
        // Arrange
        setupAuthenticatedUser();
        when(subscriptionRepository.findSubscriptionStatusByUserId(USER_ID))
                .thenReturn(Optional.of(SubscriptionStatus.CANCELED));

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.PAYMENT_REQUIRED.value());
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("PAYMENT_REQUIRED");
    }

    // Utility method to avoid code repetition
    private void setupAuthenticatedUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(testUser);
    }
}