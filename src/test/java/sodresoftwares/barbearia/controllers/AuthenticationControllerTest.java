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
import sodresoftwares.barbearia.dto.AuthenticationDTO;
import sodresoftwares.barbearia.dto.LoginResponseDTO;
import sodresoftwares.barbearia.dto.RegisterDTO;
import sodresoftwares.barbearia.dto.RegisterProfessionalDTO;
import sodresoftwares.barbearia.infra.security.SecurityFilter;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.services.AuthenticationService;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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

    private AuthenticationDTO authenticationDTO;
    private RegisterDTO registerDTO;
    private RegisterProfessionalDTO registerProfessionalDTO;

    @BeforeEach
    void setUp() {
        authenticationDTO = new AuthenticationDTO("user@test.com", "password123");
        registerDTO = new RegisterDTO(
                "user@test.com",
                "password123",
                "Cliente Teste",
                "11999999999",
                UserRole.USER)
        ;
        registerProfessionalDTO = new RegisterProfessionalDTO(
                "barber@test.com",
                "password123",
                "Barbeiro Teste",
                "11999999999",
                "Barbearia do Zé"
        );
    }

    // ==================== LOGIN TESTS ====================

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

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("Should register new user successfully (HTTP 201)")
    void testRegister_Success() throws Exception {
        // Arrange
        User userMock = new User();
        when(authService.register(any(RegisterDTO.class))).thenReturn(userMock);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(registerDTO).getJson()))
                .andExpect(status().isCreated());

        verify(authService).register(any(RegisterDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when register fields are blank")
    void testRegister_ValidationErrors() throws Exception {
        RegisterDTO invalidDTO = new RegisterDTO("", "", "", "123", null);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(invalidDTO).getJson()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // ==================== REGISTER PROFESSIONAL TESTS ====================

    @Test
    @DisplayName("Should register new professional successfully (HTTP 201)")
    void testRegisterProfessional_Success() throws Exception {
        // Arrange
        doNothing().when(authService).registerProfessional(any(RegisterProfessionalDTO.class));

        // Act & Assert
        mockMvc.perform(post("/auth/register/professional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(registerProfessionalDTO).getJson()))
                .andExpect(status().isCreated());

        verify(authService).registerProfessional(any(RegisterProfessionalDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when professional register fields are blank")
    void testRegisterProfessional_ValidationErrors() throws Exception {
        // Arrange
        RegisterProfessionalDTO invalidDTO = new RegisterProfessionalDTO("", "", "", "123", "");

        // Act & Assert
        mockMvc.perform(post("/auth/register/professional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(invalidDTO).getJson()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}