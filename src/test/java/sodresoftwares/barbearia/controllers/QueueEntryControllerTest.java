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
import sodresoftwares.barbearia.dto.JoinQueueDTO;
import sodresoftwares.barbearia.dto.QueueEntryResponseDTO;
import sodresoftwares.barbearia.infra.security.SecurityFilter;
import sodresoftwares.barbearia.model.QueueEntryStatus;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.services.QueueEntryService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = QueueEntryController.class,
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
@DisplayName("QueueEntryController Tests")
class QueueEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JacksonTester<JoinQueueDTO> joinQueueJson;

    @MockitoBean
    private CacheManager cacheManager;

    @MockitoBean
    private QueueEntryService queueEntryService;

    private QueueEntryResponseDTO entryResponseDTO;
    private JoinQueueDTO joinQueueDTO;

    @BeforeEach
    void setUp() {
        User loggedInUser = User.builder()
                .id("user-123")
                .name("Cliente Silva")
                .role(UserRole.USER)
                .build();

        joinQueueDTO = new JoinQueueDTO("session-789", "Corte de Cabelo");

        entryResponseDTO = new QueueEntryResponseDTO(
                "entry-123",
                3,
                "user-123",
                "Cliente Silva",
                "Corte de Cabelo",
                QueueEntryStatus.WAITING,
                null
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loggedInUser, null, loggedInUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== GET ACTIVE ENTRY TESTS ====================

    @Test
    @DisplayName("GET /api/queue-entries/active/me -> Should return active entry and 200 OK")
    void testGetActiveEntry_Found() throws Exception {
        when(queueEntryService.findActiveEntryByUserId(any())).thenReturn(Optional.of(entryResponseDTO));

        mockMvc.perform(get("/api/queue-entries/active/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("entry-123"))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("GET /api/queue-entries/active/me -> Should return 204 No Content when no active entry exists")
    void testGetActiveEntry_NotFound() throws Exception {
        when(queueEntryService.findActiveEntryByUserId(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/queue-entries/active/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    // ==================== GET LATEST ENTRY TESTS ====================

    @Test
    @DisplayName("GET /api/queue-entries/latest/me -> Should return recent entry and 200 OK")
    void testGetLatestEntry_Found() throws Exception {
        // Arrange
        when(queueEntryService.findLatestEntryByUserId(any())).thenReturn(Optional.of(entryResponseDTO));

        // Act & Assert
        mockMvc.perform(get("/api/queue-entries/latest/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("entry-123"))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("GET /api/queue-entries/latest/me -> Should return 204 No Content when no recent entry exists")
    void testGetLatestEntry_NotFound() throws Exception {
        // Arrange
        when(queueEntryService.findLatestEntryByUserId(any())).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/queue-entries/latest/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    // ==================== JOIN QUEUE TESTS ====================

    @Test
    @DisplayName("POST /api/queue-entries/join -> Should join queue and return 201 Created")
    void testJoinQueue_Success() throws Exception {
        when(queueEntryService.joinQueue(any(), any())).thenReturn(entryResponseDTO);

        mockMvc.perform(post("/api/queue-entries/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinQueueJson.write(joinQueueDTO).getJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("entry-123"))
                .andExpect(jsonPath("$.position").value(3))
                .andExpect(jsonPath("$.clientName").value("Cliente Silva"))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("POST /api/queue-entries/join -> Should return 400 Bad Request when DTO is invalid")
    void testJoinQueue_ValidationError() throws Exception {
        JoinQueueDTO invalidDTO = new JoinQueueDTO(null, "");

        mockMvc.perform(post("/api/queue-entries/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinQueueJson.write(invalidDTO).getJson()))
                .andExpect(status().isBadRequest());
    }

    // ==================== CALL NEXT TESTS ====================

    @Test
    @DisplayName("POST /api/queue-entries/session/{sessionId}/call-next -> Should call next and return 200 OK")
    void testCallNext_Success() throws Exception {
        QueueEntryResponseDTO calledEntry = new QueueEntryResponseDTO(
                "entry-123",
                1,
                "user-123",
                "Cliente Silva",
                "Corte de Cabelo",
                QueueEntryStatus.CALLED,
                null
        );

        when(queueEntryService.callNext(any(), any())).thenReturn(calledEntry);

        mockMvc.perform(post("/api/queue-entries/session/{sessionId}/call-next", "session-789")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("entry-123"))
                .andExpect(jsonPath("$.status").value("CALLED"));
    }

    // ==================== REQUEUE ENTRY TESTS ====================

    @Test
    @DisplayName("POST /api/queue-entries/{entryId}/requeue -> Should requeue entry and return 200 OK")
    void testRequeueEntry_Success() throws Exception {
        when(queueEntryService.requeueEntry(any(), any())).thenReturn(entryResponseDTO);

        mockMvc.perform(post("/api/queue-entries/{entryId}/requeue", "entry-123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("entry-123"));
    }

    // ==================== START SERVICE TESTS ====================

    @Test
    @DisplayName("POST /api/queue-entries/{entryId}/start -> Should start service and return 200 OK")
    void testStartService_Success() throws Exception {
        QueueEntryResponseDTO inServiceEntry = new QueueEntryResponseDTO(
                "entry-123",
                1,
                "user-123",
                "Cliente Silva",
                "Corte de Cabelo",
                QueueEntryStatus.IN_SERVICE,
                null
        );
        when(queueEntryService.startService(any(), any())).thenReturn(inServiceEntry);

        mockMvc.perform(post("/api/queue-entries/{entryId}/start", "entry-123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("entry-123"))
                .andExpect(jsonPath("$.status").value("IN_SERVICE"));
    }

    // ==================== FINISH SERVICE TESTS ====================

    @Test
    @DisplayName("POST /api/queue-entries/{entryId}/finish -> Should finish service and return 204 No Content")
    void testFinishService_Success() throws Exception {
        doNothing().when(queueEntryService).finishService(any(), any());

        mockMvc.perform(post("/api/queue-entries/{entryId}/finish", "entry-123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    // ==================== CANCEL ENTRY TESTS ====================

    @Test
    @DisplayName("POST /api/queue-entries/{entryId}/cancel -> Should cancel entry and return 204 No Content")
    void testCancelEntry_Success() throws Exception {
        doNothing().when(queueEntryService).cancelEntry(any(), any());

        mockMvc.perform(post("/api/queue-entries/{entryId}/cancel", "entry-123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}