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
import sodresoftwares.barbearia.dto.UpdateProfessionalDTO;
import sodresoftwares.barbearia.infra.security.SecurityFilter;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.services.ProfessionalService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private JacksonTester<UpdateProfessionalDTO> updateProfessionalJson;

    @MockitoBean
    private ProfessionalService professionalService;

    @MockitoBean
    private CacheManager cacheManager;

    private User loggedInUser;
    private UpdateProfessionalDTO updateDTO;
    private ProfessionalResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        loggedInUser = User.builder()
                .id("barber-user-123")
                .name("Barbeiro Zé")
                .role(UserRole.PROFESSIONAL)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loggedInUser, null, loggedInUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        updateDTO = new UpdateProfessionalDTO("New Business Name");

        responseDTO = new ProfessionalResponseDTO(
                "prof-123",
                "New Business Name",
                true
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== UPDATE PROFESSIONAL PROFILE TESTS ====================

    @Test
    @DisplayName("PATCH /api/professionals/me -> Should update business profile and return 200 OK")
    void testUpdateMyProfessionalProfile_Success() throws Exception {
        when(professionalService.updateProfessionalProfile(any(), any()))
                .thenReturn(responseDTO);

        mockMvc.perform(patch("/api/professionals/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateProfessionalJson.write(updateDTO).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prof-123"))
                .andExpect(jsonPath("$.businessName").value("New Business Name"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("PATCH /api/professionals/me -> Should return 400 Bad Request when DTO has validation errors")
    void testUpdateMyProfessionalProfile_ValidationError() throws Exception {
        UpdateProfessionalDTO invalidDTO = new UpdateProfessionalDTO("A");

        mockMvc.perform(patch("/api/professionals/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateProfessionalJson.write(invalidDTO).getJson()))
                .andExpect(status().isBadRequest());
    }
}