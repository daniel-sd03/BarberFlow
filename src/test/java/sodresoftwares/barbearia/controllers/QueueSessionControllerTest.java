package sodresoftwares.barbearia.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sodresoftwares.barbearia.dto.ProfessionalDashboardDTO;
import sodresoftwares.barbearia.dto.QueueEntryResponseDTO;
import sodresoftwares.barbearia.dto.QueueSessionResponseDTO;
import sodresoftwares.barbearia.dto.UpdateQueueStatusDTO;
import sodresoftwares.barbearia.infra.security.SecurityFilter;
import sodresoftwares.barbearia.model.QueueEntryStatus;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.services.QueueSessionService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = QueueSessionController.class,
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
@DisplayName("QueueSessionController Tests")
class QueueSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JacksonTester<Object> jsonTester;

    @MockitoBean
    private CacheManager cacheManager;

    @MockitoBean
    private QueueSessionService queueSessionService;

    private User loggedInUser;
    private QueueSessionResponseDTO sessionResponseDTO;
    private ProfessionalDashboardDTO dashboardDTO;

    @BeforeEach
    void setUp() {
        String USER_ID = "prof-user-123";
        loggedInUser = User.builder()
                .id(USER_ID)
                .name("Barbeiro Zé")
                .role(UserRole.PROFESSIONAL)
                .build();

        sessionResponseDTO = new QueueSessionResponseDTO(
                "session-123",
                "BARB1234",
                false
        );

        QueueEntryResponseDTO entryDTO = new QueueEntryResponseDTO(
                "entry-123",
                1,
                "client-123",
                "João Silva",
                "Corte Navalhado",
                QueueEntryStatus.WAITING
        );

        dashboardDTO = new ProfessionalDashboardDTO(
                "session-123",
                "Barbearia do Zé",
                "BARB1234",
                true,
                List.of(entryDTO)
        );
    }

    // ==================== CREATE SESSION TESTS ====================
    @Test
    @DisplayName("POST /api/queue-sessions -> Should create session and return 201 Created")
    void testCreateSession_Success() throws Exception {
        // Arrange
        when(queueSessionService.createQueueSession(any())).thenReturn(sessionResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/queue-sessions")
                        .with(user(loggedInUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("session-123"))
                .andExpect(jsonPath("$.ticketCode").value("BARB1234"))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    // ==================== UPDATE STATUS TESTS ====================

    @Test
    @DisplayName("PATCH /api/queue-sessions/status -> Should update status and return 200 OK")
    void testUpdateStatus_Success() throws Exception {
        // Arrange
        UpdateQueueStatusDTO requestDTO = new UpdateQueueStatusDTO(true);
        QueueSessionResponseDTO activeSessionDTO = new QueueSessionResponseDTO("session-123", "BARB1234", true);

        when(queueSessionService.updateQueueStatus(any(), anyBoolean())).thenReturn(activeSessionDTO);

        // Act & Assert
        mockMvc.perform(patch("/api/queue-sessions/status")
                        .with(user(loggedInUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(requestDTO).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("session-123"))
                .andExpect(jsonPath("$.ticketCode").value("BARB1234"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("PATCH /api/queue-sessions/status -> Should return 400 Bad Request when body is invalid")
    void testUpdateStatus_ValidationError() throws Exception {
        // Arrange
        UpdateQueueStatusDTO requestDTO = new UpdateQueueStatusDTO(null);

        // Act & Assert
        mockMvc.perform(patch("/api/queue-sessions/status")
                        .with(user(loggedInUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(requestDTO).getJson()))
                .andExpect(status().isBadRequest());
    }

    // ==================== DASHBOARD TESTS ====================

    @Test
    @DisplayName("GET /api/queue-sessions/dashboard -> Should return dashboard payload and 200 OK")
    void testGetDashboard_Success() throws Exception {
        // Arrange
        when(queueSessionService.getDashboardData(any())).thenReturn(dashboardDTO);

        // Act & Assert
        mockMvc.perform(get("/api/queue-sessions/dashboard")
                        .with(user(loggedInUser))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-123"))
                .andExpect(jsonPath("$.businessName").value("Barbearia do Zé"))
                .andExpect(jsonPath("$.ticketCode").value("BARB1234"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.activeQueue[0].id").value("entry-123"))
                .andExpect(jsonPath("$.activeQueue[0].position").value(1))
                .andExpect(jsonPath("$.activeQueue[0].userId").value("client-123"))
                .andExpect(jsonPath("$.activeQueue[0].clientName").value("João Silva"))
                .andExpect(jsonPath("$.activeQueue[0].serviceName").value("Corte Navalhado"))
                .andExpect(jsonPath("$.activeQueue[0].status").value("WAITING"));
    }
}