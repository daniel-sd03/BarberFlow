package sodresoftwares.barbearia.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sodresoftwares.barbearia.dto.ProfessionalResponseDTO;
import sodresoftwares.barbearia.dto.RegisterProfessionalDTO;
import sodresoftwares.barbearia.dto.UpdateProfessionalDTO;
import sodresoftwares.barbearia.dto.UserResponseDTO;
import sodresoftwares.barbearia.infra.security.SecurityFilter;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.services.ProfessionalService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProfessionalController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = SecurityFilter.class
                )
        },
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
@DisplayName("ProfessionalController Tests")
class ProfessionalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JacksonTester<Object> jsonTester;

    @MockitoBean
    private ProfessionalService professionalService;

    @MockitoBean
    private CacheManager cacheManager;

    private User loggedInUser;
    private RegisterProfessionalDTO registerProfessionalDTO;
    private UpdateProfessionalDTO updateDTO;
    private ProfessionalResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        loggedInUser = User.builder()
                .id("barber-user-123")
                .name("Barbeiro Zé")
                .role(UserRole.PROFESSIONAL)
                .build();

        registerProfessionalDTO = new RegisterProfessionalDTO(
                "barber@test.com",
                "password123",
                "Barbeiro Teste",
                "11999999999",
                "Barbearia do Zé"
        );

        updateDTO = new UpdateProfessionalDTO("New Business Name");

        UserResponseDTO userDTO = UserResponseDTO.fromEntity(loggedInUser);

        responseDTO = new ProfessionalResponseDTO(
                "prof-123",
                "New Business Name",
                true,
                userDTO
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== GET MY PROFESSIONAL PROFILE TESTS ====================

    @Test
    @DisplayName("GET /professionals/me -> Should return 200 OK and profile data including user")
    void testGetMyProfessionalProfile_Success() throws Exception {
        // Arrange
        UserResponseDTO mockUserDTO = new UserResponseDTO("user-123", "Barbeiro Zé", "ze@test.com", "3199999999", "PROFESSIONAL");
        ProfessionalResponseDTO getResponseDTO = new ProfessionalResponseDTO("prof-123", "Barbearia do Zé", true, mockUserDTO);

        when(professionalService.getMyProfessionalProfile(any())).thenReturn(getResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/professionals/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prof-123"))
                .andExpect(jsonPath("$.businessName").value("Barbearia do Zé"))
                .andExpect(jsonPath("$.user.id").value("user-123"))
                .andExpect(jsonPath("$.user.name").value("Barbeiro Zé"))
                .andExpect(jsonPath("$.user.login").value("ze@test.com"));
    }

    // ==================== POST REGISTER PROFESSIONAL TESTS ====================

    @Test
    @DisplayName("POST /professionals -> Should register new professional successfully (HTTP 201)")
    void testRegisterProfessional_Success() throws Exception {
        // Arrange
        doNothing().when(professionalService).registerProfessional(any(RegisterProfessionalDTO.class));

        // Act & Assert
        mockMvc.perform(post("/professionals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(registerProfessionalDTO).getJson()))
                .andExpect(status().isCreated());

        verify(professionalService).registerProfessional(any(RegisterProfessionalDTO.class));
    }

    @Test
    @DisplayName("POST /professionals -> Should return 400 when professional register fields are blank")
    void testRegisterProfessional_ValidationErrors() throws Exception {
        // Arrange
        RegisterProfessionalDTO invalidDTO = new RegisterProfessionalDTO(
                "", "", "", "123", "");

        // Act & Assert
        mockMvc.perform(post("/professionals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(invalidDTO).getJson()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(professionalService);
    }

    // ==================== PATCH UPDATE PROFESSIONAL PROFILE TESTS ====================

    @Test
    @DisplayName("PATCH /professionals/me -> Should update business profile and return 200 OK")
    void testUpdateMyProfessionalProfile_Success() throws Exception {
        when(professionalService.updateProfessionalProfile(any(), any()))
                .thenReturn(responseDTO);

        mockMvc.perform(patch("/professionals/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(updateDTO).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prof-123"))
                .andExpect(jsonPath("$.businessName").value("New Business Name"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("PATCH /professionals/me -> Should return 400 Bad Request when DTO has validation errors")
    void testUpdateMyProfessionalProfile_ValidationError() throws Exception {
        UpdateProfessionalDTO invalidDTO = new UpdateProfessionalDTO("A");

        mockMvc.perform(patch("/professionals/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(invalidDTO).getJson()))
                .andExpect(status().isBadRequest());
    }
}