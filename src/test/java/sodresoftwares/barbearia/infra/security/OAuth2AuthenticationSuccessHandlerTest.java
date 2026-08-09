package sodresoftwares.barbearia.infra.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import sodresoftwares.barbearia.model.LgpdConsent;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.repositories.LgpdConsentRepository;
import sodresoftwares.barbearia.repositories.UserRepository;
import sodresoftwares.barbearia.services.LgpdConsentService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LgpdConsentRepository lgpdConsentRepository;

    @Mock
    private LgpdConsentService lgpdConsentService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler successHandler;

    private final String email = "user@example.com";
    private final String googleId = "1234567890";
    private final String fakeToken = "mocked-jwt-token";
    private final String fakeLgpdVersion = "1.0";
    private LgpdConsent mockConsent;

    @BeforeEach
    void setUp() {
        mockConsent = LgpdConsent.builder().termVersion(fakeLgpdVersion).build();
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn(email);
        when(oAuth2User.getAttribute("sub")).thenReturn(googleId);
        when(response.encodeRedirectURL(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Scenario 1: Should log in existing Google user and sync email if changed")
    void onAuthenticationSuccess_Scenario1_ExistingGoogleUser() throws Exception {
        // Arrange
        User existingUser = User.builder()
                .login("old-email@example.com")
                .googleId(googleId)
                .role(UserRole.USER)
                .build();

        when(userRepository.findByGoogleId(googleId)).thenReturn(existingUser);
        when(lgpdConsentRepository.findFirstByUserIdOrderByCreatedAtDesc(existingUser.getId()))
                .thenReturn(Optional.of(mockConsent));
        when(tokenService.generateToken(existingUser, fakeLgpdVersion)).thenReturn(fakeToken);

        // Act
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        assertEquals(email, existingUser.getLogin());
        verify(userRepository).save(existingUser);
        verify(tokenService).generateToken(existingUser, fakeLgpdVersion);
        verify(lgpdConsentService, never()).registerConsentForNewUser(any(), any());

        assertCookieAndRedirect();
    }

    @Test
    @DisplayName("Scenario 2: Should link Google ID to existing standard user by email")
    void onAuthenticationSuccess_Scenario2_LinkExistingUserByEmail() throws Exception {
        // Arrange
        User standardUser = User.builder()
                .login(email)
                .googleId(null)
                .role(UserRole.USER)
                .build();

        when(userRepository.findByGoogleId(googleId)).thenReturn(null);
        when(userRepository.findByLogin(email)).thenReturn(standardUser);
        when(lgpdConsentRepository.findFirstByUserIdOrderByCreatedAtDesc(standardUser.getId()))
                .thenReturn(Optional.of(mockConsent));
        when(tokenService.generateToken(standardUser, fakeLgpdVersion)).thenReturn(fakeToken);

        // Act
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        assertEquals(googleId, standardUser.getGoogleId());
        verify(userRepository).save(standardUser);
        verify(tokenService).generateToken(standardUser, fakeLgpdVersion);
        verify(lgpdConsentService, never()).registerConsentForNewUser(any(), any());

        assertCookieAndRedirect();
    }

    @Test
    @DisplayName("Scenario 3: Should create a brand new user when no account matches")
    void onAuthenticationSuccess_Scenario3_CreateNewUser() throws Exception {
        // Arrange
        when(userRepository.findByGoogleId(googleId)).thenReturn(null);
        when(userRepository.findByLogin(email)).thenReturn(null);
        when(lgpdConsentRepository.findFirstByUserIdOrderByCreatedAtDesc(anyString()))
                .thenReturn(Optional.of(mockConsent));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId("new-user-123");
            return u;
        });
        when(tokenService.generateToken(any(User.class), eq(fakeLgpdVersion))).thenReturn(fakeToken);

        // Act
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        User newUser = userCaptor.getValue();
        assertNotNull(newUser);
        assertEquals(email, newUser.getLogin());
        assertEquals(googleId, newUser.getGoogleId());
        assertEquals(UserRole.USER, newUser.getRole());
        assertNotNull(newUser.getPassword());

        verify(userRepository).save(newUser);
        verify(lgpdConsentService).registerConsentForNewUser(newUser, request);
        verify(tokenService).generateToken(newUser, fakeLgpdVersion);

        assertCookieAndRedirect();
    }

    private void assertCookieAndRedirect() throws Exception {
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

        verify(response, times(2)).addCookie(cookieCaptor.capture());

        List<Cookie> capturedCookies = cookieCaptor.getAllValues();
        assertEquals(2, capturedCookies.size());

        Cookie tokenCookie = capturedCookies.stream()
                .filter(c -> "TEMP_AUTH_TOKEN".equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("TEMP_AUTH_TOKEN cookie not found"));

        assertEquals(fakeToken, tokenCookie.getValue());
        assertEquals("/", tokenCookie.getPath());
        assertEquals(60, tokenCookie.getMaxAge());

        Cookie roleCookie = capturedCookies.stream()
                .filter(c -> "TEMP_ROLE".equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("TEMP_ROLE cookie not found"));

        assertEquals(UserRole.USER.name(), roleCookie.getValue());
        assertEquals("/", roleCookie.getPath());
        assertEquals(60, roleCookie.getMaxAge());

        verify(response).sendRedirect("http://localhost:5173/inicio");
    }
}