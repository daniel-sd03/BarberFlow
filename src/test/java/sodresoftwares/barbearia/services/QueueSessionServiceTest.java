package sodresoftwares.barbearia.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import sodresoftwares.barbearia.dto.ProfessionalDashboardDTO;
import sodresoftwares.barbearia.dto.QueueSessionResponseDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.Professional;
import sodresoftwares.barbearia.model.QueueEntry;
import sodresoftwares.barbearia.model.QueueEntryStatus;
import sodresoftwares.barbearia.model.QueueSession;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.repositories.ProfessionalRepository;
import sodresoftwares.barbearia.repositories.QueueSessionRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueSessionService Tests")
class QueueSessionServiceTest {

    @Mock
    private QueueSessionRepository queueSessionRepository;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private QueueCacheService queueCacheService;

    @InjectMocks
    private QueueSessionService queueSessionService;

    private Professional professional;
    private QueueSession existingSession;
    private QueueEntry activeEntry;

    private final String PROF_USER_ID = "prof-user-123";
    private final String CUSTOMER_USER_ID = "customer-user-123";

    @BeforeEach
    void setUp() {
        User professionalUser = User.builder()
                .id(PROF_USER_ID)
                .name("Barbeiro Zé")
                .role(UserRole.PROFESSIONAL)
                .build();

        User customerUser = User.builder()
                .id(CUSTOMER_USER_ID)
                .name("Cliente João")
                .role(UserRole.USER)
                .build();

        professional = Professional.builder()
                .id("prof-123")
                .user(professionalUser)
                .businessName("Barbearia do Zé")
                .isActive(true)
                .build();

        existingSession = QueueSession.builder()
                .id("session-123")
                .professional(professional)
                .ticketCode("BARBEA1234")
                .isActive(false)
                .build();

        activeEntry = QueueEntry.builder()
                .id("entry-123")
                .queueSession(existingSession)
                .user(customerUser)
                .serviceName("Corte Navalhado")
                .status(QueueEntryStatus.WAITING)
                .joinedAt(Instant.now())
                .build();
    }

    // ==================== CREATE QUEUE SESSION TESTS ====================

    @Test
    @DisplayName("Should create queue session and generate prefix based on business name")
    void testCreateQueueSession_Success_PrefixGeneration() {
        // Arrange
        when(queueSessionRepository.existsByProfessionalId(PROF_USER_ID)).thenReturn(false);
        when(professionalRepository.findByUserId(PROF_USER_ID)).thenReturn(Optional.of(professional));
        when(queueSessionRepository.existsByTicketCode(anyString())).thenReturn(false);
        when(queueSessionRepository.save(any(QueueSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        QueueSessionResponseDTO result = queueSessionService.createQueueSession(PROF_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isActive()).isFalse();
        assertThat(result.ticketCode()).startsWith("BARB");

        verify(queueSessionRepository).save(any(QueueSession.class));
    }

    @Test
    @DisplayName("Should generate a new ticket code if a collision is detected during creation")
    void testCreateQueueSession_CollisionLoop() {
        // Arrange
        when(queueSessionRepository.existsByProfessionalId(PROF_USER_ID)).thenReturn(false);
        when(professionalRepository.findByUserId(PROF_USER_ID)).thenReturn(Optional.of(professional));
        when(queueSessionRepository.existsByTicketCode(anyString())).thenReturn(true, false);
        when(queueSessionRepository.save(any(QueueSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        queueSessionService.createQueueSession(PROF_USER_ID);

        // Assert
        verify(queueSessionRepository, times(2)).existsByTicketCode(anyString());
    }

    @Test
    @DisplayName("Should throw conflict exception when professional already has a queue")
    void testCreateQueueSession_AlreadyExists() {
        // Arrange
        when(queueSessionRepository.existsByProfessionalId(PROF_USER_ID)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> queueSessionService.createQueueSession(PROF_USER_ID))
                .isInstanceOf(AppException.class)
                .hasMessage("This professional already has a queue session.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);

        verify(queueSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when creating queue but professional is not found")
    void testCreateQueueSession_ProfessionalNotFound() {
        // Arrange
        when(queueSessionRepository.existsByProfessionalId(PROF_USER_ID)).thenReturn(false);
        when(professionalRepository.findByUserId(PROF_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> queueSessionService.createQueueSession(PROF_USER_ID))
                .isInstanceOf(AppException.class)
                .hasMessage("Professional not found")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);

        verify(queueSessionRepository, never()).save(any());
        verify(queueSessionRepository, never()).existsByTicketCode(anyString());
    }

    // ====================  UPDATE QUEUE STATUS TESTS ====================

    @Test
    @DisplayName("Should update an existing queue session successfully")
    void testUpdateQueueStatus_Success() {
        // Arrange
        when(queueSessionRepository.findByProfessionalId(PROF_USER_ID)).thenReturn(Optional.of(existingSession));
        when(queueSessionRepository.save(any(QueueSession.class))).thenReturn(existingSession);

        // Act
        QueueSessionResponseDTO result = queueSessionService.updateQueueStatus(PROF_USER_ID, true);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isActive()).isTrue();
        assertThat(result.id()).isEqualTo(existingSession.getId());

        verify(queueSessionRepository).save(existingSession);
    }

    @Test
    @DisplayName("Should throw exception when updating status but session does not exist")
    void testUpdateQueueStatus_SessionNotFound() {
        // Arrange
        when(queueSessionRepository.findByProfessionalId(PROF_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> queueSessionService.updateQueueStatus(PROF_USER_ID, true))
                .isInstanceOf(AppException.class)
                .hasMessage("Queue not set up yet.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);

        verify(queueSessionRepository, never()).save(any());
    }

    // ==================== DASHBOARD TESTS ====================

    @Test
    @DisplayName("Should return active Dashboard DTO successfully using Cache")
    void testGetDashboardData_SessionExists() {
        // Arrange
        when(queueSessionRepository.findByProfessionalId(PROF_USER_ID)).thenReturn(Optional.of(existingSession));
        when(queueCacheService.getActiveEntries(existingSession.getId())).thenReturn(List.of(activeEntry));

        // Act
        ProfessionalDashboardDTO dashboardData = queueSessionService.getDashboardData(PROF_USER_ID);

        // Assert
        assertThat(dashboardData).isNotNull();
        assertThat(dashboardData.sessionId()).isEqualTo(existingSession.getId());
        assertThat(dashboardData.businessName()).isEqualTo("Barbearia do Zé");
        assertThat(dashboardData.ticketCode()).isEqualTo("BARBEA1234");
        assertThat(dashboardData.isActive()).isFalse();

        assertThat(dashboardData.activeQueue()).hasSize(1);
        var firstEntry = dashboardData.activeQueue().get(0);

        assertThat(firstEntry.id()).isEqualTo("entry-123");
        assertThat(firstEntry.position()).isEqualTo(1);
        assertThat(firstEntry.userId()).isEqualTo(CUSTOMER_USER_ID);
        assertThat(firstEntry.clientName()).isEqualTo("Cliente João");
        assertThat(firstEntry.serviceName()).isEqualTo("Corte Navalhado");
        assertThat(firstEntry.status()).isEqualTo(QueueEntryStatus.WAITING);
    }

    @Test
    @DisplayName("Should return blank Dashboard DTO if session does not exist yet (First Access)")
    void testGetDashboardData_FirstAccess() {
        // Arrange
        when(queueSessionRepository.findByProfessionalId(PROF_USER_ID)).thenReturn(Optional.empty());
        when(professionalRepository.findByUserId(PROF_USER_ID)).thenReturn(Optional.of(professional));

        // Act
        ProfessionalDashboardDTO dashboardData = queueSessionService.getDashboardData(PROF_USER_ID);

        // Assert
        assertThat(dashboardData).isNotNull();
        assertThat(dashboardData.sessionId()).isNull();
        assertThat(dashboardData.ticketCode()).isNull();
        assertThat(dashboardData.isActive()).isFalse();
        assertThat(dashboardData.businessName()).isEqualTo("Barbearia do Zé");
        assertThat(dashboardData.activeQueue()).isEmpty();

        verifyNoInteractions(queueCacheService);
    }
}