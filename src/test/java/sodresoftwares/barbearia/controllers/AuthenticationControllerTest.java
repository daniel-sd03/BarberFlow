package sodresoftwares.barbearia.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sodresoftwares.barbearia.dto.*;
import sodresoftwares.barbearia.infra.security.SecurityFilter;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.services.AuthenticationService;
import sodresoftwares.barbearia.services.PasswordResetService;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthenticationController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = SecurityFilter.class
                )
        },
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
@DisplayName("AuthenticationController Tests")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CacheManager cacheManager;

    @Autowired
    private JacksonTester<Object> jsonTester;

    @MockitoBean
    private AuthenticationService authService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    private AuthenticationDTO authenticationDTO;
    private ForgotPasswordDTO forgotPasswordDTO;
    private ValidateTokenDTO validateTokenDTO;
    private ResetPasswordDTO resetPasswordDTO;

    @BeforeEach
    void setUp() {
        authenticationDTO = new AuthenticationDTO("user@test.com", "password123");
        forgotPasswordDTO = new ForgotPasswordDTO("user@test.com");
        validateTokenDTO = new ValidateTokenDTO("user@test.com", "123456");
        resetPasswordDTO = new ResetPasswordDTO("user@test.com", "123456", "newPass123", "newPass123");
    }

    // ==================== POST LOGIN TESTS ====================

    @Test
    @DisplayName("Should login successfully and return token (HTTP 200)")
    void testLogin_Success() throws Exception {
        // Arrange
        String VALID_TOKEN = "jwt-token-example";
        String VALID_ROLE = UserRole.USER.toString();

        LoginResponseDTO mockResponse = new LoginResponseDTO(VALID_TOKEN, VALID_ROLE);

        when(authService.login(any(AuthenticationDTO.class))).thenReturn(mockResponse);

        // Act & Assert
            mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(authenticationDTO).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(VALID_TOKEN)))
                .andExpect(jsonPath("$.role", is(VALID_ROLE)));

        verify(authService).login(any(AuthenticationDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when login or password are blank")
    void testLogin_ValidationErrors() throws Exception {
        // Arrange
        AuthenticationDTO invalidDTO = new AuthenticationDTO("", "");

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(invalidDTO).getJson()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // ==================== POST FORGOT PASSWORD TESTS ====================

    @Test
    @DisplayName("Should request password reset successfully (HTTP 200)")
    void testForgotPassword_Success() throws Exception {
        // Arrange
        doNothing().when(passwordResetService).requestPasswordReset(anyString());

        // Act & Assert
        mockMvc.perform(post("/auth/password-resets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(forgotPasswordDTO).getJson()))
                .andExpect(status().isOk());

        verify(passwordResetService).requestPasswordReset(forgotPasswordDTO.email());
    }

    @Test
    @DisplayName("Should return 400 when forgot password email is blank or invalid")
    void testForgotPassword_ValidationErrors() throws Exception {
        // Arrange
        ForgotPasswordDTO invalidDTO = new ForgotPasswordDTO("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/auth/password-resets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(invalidDTO).getJson()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passwordResetService);
    }

    // ==================== POST VALIDATE TOKEN TESTS ====================

    @Test
    @DisplayName("Should validate token successfully (HTTP 200)")
    void testValidateToken_Success() throws Exception {
        // Arrange
        doNothing().when(passwordResetService).validateToken(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post("/auth/password-resets/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(validateTokenDTO).getJson()))
                .andExpect(status().isOk());

        verify(passwordResetService).validateToken(validateTokenDTO.email(), validateTokenDTO.code());
    }

    @Test
    @DisplayName("Should return 400 when validate token fields are blank")
    void testValidateToken_ValidationErrors() throws Exception {
        // Arrange
        ValidateTokenDTO invalidDTO = new ValidateTokenDTO("", "");

        // Act & Assert
        mockMvc.perform(post("/auth/password-resets/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(invalidDTO).getJson()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passwordResetService);
    }

    // ==================== PATCH RESET PASSWORD TESTS ====================

    @Test
    @DisplayName("Should reset password successfully (HTTP 204)")
    void testResetPassword_Success() throws Exception {
        // Arrange
        doNothing().when(passwordResetService).resetPassword(any(ResetPasswordDTO.class));

        // Act & Assert
        mockMvc.perform(patch("/auth/passwords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(resetPasswordDTO).getJson()))
                .andExpect(status().isNoContent());

        verify(passwordResetService).resetPassword(any(ResetPasswordDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when reset password fields are blank")
    void testResetPassword_ValidationErrors() throws Exception {
        // Arrange
        ResetPasswordDTO invalidDTO = new ResetPasswordDTO("", "", "", "");

        // Act & Assert
        mockMvc.perform(patch("/auth/passwords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(invalidDTO).getJson()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passwordResetService);
    }
}
