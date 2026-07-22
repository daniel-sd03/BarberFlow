package sodresoftwares.barbearia.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import sodresoftwares.barbearia.dto.JoinQueueDTO;
import sodresoftwares.barbearia.dto.QueueEntryResponseDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.mappers.QueueMapper;
import sodresoftwares.barbearia.model.Professional;
import sodresoftwares.barbearia.model.QueueEntry;
import sodresoftwares.barbearia.model.QueueEntryStatus;
import sodresoftwares.barbearia.model.QueueSession;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.repositories.QueueEntryRepository;
import sodresoftwares.barbearia.repositories.QueueSessionRepository;
import sodresoftwares.barbearia.repositories.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueEntryService Tests")
class QueueEntryServiceTest {

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private QueueSessionRepository queueSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QueueCacheService queueCacheService;

    @Mock
    private QueueNotificationService queueNotificationService;

    @Spy
    private QueueMapper queueMapper = new QueueMapper();

    @InjectMocks
    private QueueEntryService queueEntryService;

    private User clientUser;
    private Professional professional;
    private QueueSession activeSession;
    private QueueEntry waitingEntry;
    private JoinQueueDTO joinQueueDTO;

    private final String BARBER_USER_ID = "barber-user-123";
    private final String CLIENT_USER_ID = "client-user-123";
    private final String SESSION_ID = "session-123";
    private final String ENTRY_ID = "entry-123";

    @BeforeEach
    void setUp() {
        User barberUser = User.builder().id(BARBER_USER_ID).name("Barbeiro Zé").role(UserRole.PROFESSIONAL).build();
        clientUser = User.builder().id(CLIENT_USER_ID).name("Cliente João").role(UserRole.USER).build();

        professional = Professional.builder()
                .id("prof-123")
                .user(barberUser)
                .businessName("Barbearia do Zé")
                .isActive(true)
                .build();

        activeSession = QueueSession.builder()
                .id(SESSION_ID)
                .professional(professional)
                .isActive(true)
                .build();

        waitingEntry = QueueEntry.builder()
                .id(ENTRY_ID)
                .queueSession(activeSession)
                .user(clientUser)
                .serviceName("Corte")
                .status(QueueEntryStatus.WAITING)
                .joinedAt(Instant.now())
                .build();

        joinQueueDTO = new JoinQueueDTO(SESSION_ID, "Corte");
    }

    // ==================== JOIN QUEUE TESTS ====================

    @Test
    @DisplayName("Should allow user to join queue and map all DTO fields correctly")
    void testJoinQueue_Success() {
        // Arrange
        when(queueSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession));
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(queueEntryRepository.existsByUserIdAndStatusIn(eq(CLIENT_USER_ID), anyList())).thenReturn(false);
        when(queueEntryRepository.save(any(QueueEntry.class))).thenReturn(waitingEntry);
        when(queueEntryRepository.findActiveEntriesBySessionId(SESSION_ID)).thenReturn(List.of(waitingEntry));

        // Act
        QueueEntryResponseDTO response = queueEntryService.joinQueue(joinQueueDTO, CLIENT_USER_ID);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(ENTRY_ID);
        assertThat(response.position()).isEqualTo(1);
        assertThat(response.userId()).isEqualTo(CLIENT_USER_ID);
        assertThat(response.clientName()).isEqualTo("Cliente João");
        assertThat(response.serviceName()).isEqualTo("Corte");
        assertThat(response.status()).isEqualTo(QueueEntryStatus.WAITING);

        verify(queueCacheService).evict(SESSION_ID);
    }

    @Test
    @DisplayName("Should throw exception when queue session is not found")
    void testJoinQueue_SessionNotFound() {
        // Arrange
        when(queueSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> queueEntryService.joinQueue(joinQueueDTO, CLIENT_USER_ID))
                .isInstanceOf(AppException.class)
                .hasMessage("Queue session not found")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);

        verifyNoInteractions(userRepository, queueEntryRepository);
    }

    @Test
    @DisplayName("Should block user from joining if queue is closed")
    void testJoinQueue_QueueClosed() {
        // Arrange
        activeSession.setIsActive(false);
        when(queueSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession));

        // Act & Assert
        assertThatThrownBy(() -> queueEntryService.joinQueue(joinQueueDTO, CLIENT_USER_ID))
                .isInstanceOf(AppException.class)
                .hasMessage("This queue is currently closed.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(userRepository, queueEntryRepository);
    }

    @Test
    @DisplayName("Should throw exception when user is not found")
    void testJoinQueue_UserNotFound() {
        // Arrange
        when(queueSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession));
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> queueEntryService.joinQueue(joinQueueDTO, CLIENT_USER_ID))
                .isInstanceOf(AppException.class)
                .hasMessage("User not found")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);

        verifyNoInteractions(queueEntryRepository);
    }

    @Test
    @DisplayName("Should block user from joining if already waiting in an active queue")
    void testJoinQueue_AlreadyInQueue() {
        // Arrange
        when(queueSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession));
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(queueEntryRepository.existsByUserIdAndStatusIn(eq(CLIENT_USER_ID), anyList())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> queueEntryService.joinQueue(joinQueueDTO, CLIENT_USER_ID))
                .isInstanceOf(AppException.class)
                .hasMessage("You are already waiting in an active queue.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);

        verify(queueEntryRepository, never()).save(any());
    }

    // ==================== FIND ACTIVE ENTRY TESTS ====================

    @Test
    @DisplayName("Should return active entry DTO with correct position when user is in queue")
    void testFindActiveEntryByUserId_Success() {
        // Arrange
        String userId = CLIENT_USER_ID;

        when(queueEntryRepository.findByUserIdAndStatusIn(eq(userId), anyList()))
                .thenReturn(Optional.of(waitingEntry));
        when(queueCacheService.getActiveEntries(SESSION_ID))
                .thenReturn(List.of(waitingEntry));

        // Act
        Optional<QueueEntryResponseDTO> result = queueEntryService.findActiveEntryByUserId(userId);

        // Assert
        assertThat(result).isPresent();
        QueueEntryResponseDTO dto = result.get();

        assertThat(dto.id()).isEqualTo(ENTRY_ID);
        assertThat(dto.position()).isEqualTo(1);
        assertThat(dto.userId()).isEqualTo(userId);
        assertThat(dto.clientName()).isEqualTo("Cliente João");
        assertThat(dto.serviceName()).isEqualTo("Corte");
        assertThat(dto.status()).isEqualTo(QueueEntryStatus.WAITING);


        verify(queueEntryRepository).findByUserIdAndStatusIn(eq(userId), anyList());
        verify(queueCacheService).getActiveEntries(SESSION_ID);
    }

    @Test
    @DisplayName("Should return empty optional when user has no active entries in any queue")
    void testFindActiveEntryByUserId_NotFound() {
        // Arrange
        String userId = CLIENT_USER_ID;

        when(queueEntryRepository.findByUserIdAndStatusIn(eq(userId), anyList()))
                .thenReturn(Optional.empty());

        // Act
        Optional<QueueEntryResponseDTO> result = queueEntryService.findActiveEntryByUserId(userId);

        // Assert
        assertThat(result).isEmpty();

        verify(queueEntryRepository).findByUserIdAndStatusIn(eq(userId), anyList());
        verifyNoInteractions(queueCacheService);
    }

    // ==================== CALL NEXT TESTS ====================

    @Test
    @DisplayName("Should successfully call next waiting client")
    void testCallNext_Success() {
        // Arrange
        professional.setId(BARBER_USER_ID);

        when(queueSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession));
        when(queueCacheService.getActiveEntries(SESSION_ID)).thenReturn(List.of(waitingEntry));
        when(queueEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(waitingEntry));
        when(queueEntryRepository.save(any(QueueEntry.class))).thenReturn(waitingEntry);
        when(queueEntryRepository.findActiveEntriesBySessionId(SESSION_ID)).thenReturn(List.of(waitingEntry));

        // Act
        QueueEntryResponseDTO response = queueEntryService.callNext(SESSION_ID, BARBER_USER_ID);

        // Assert
        assertThat(response).isNotNull();
        assertThat(waitingEntry.getStatus()).isEqualTo(QueueEntryStatus.CALLED);
        verify(queueCacheService).evict(SESSION_ID);
    }

    @Test
    @DisplayName("Should throw exception if a non-owner tries to call next")
    void testCallNext_Forbidden() {
        // Arrange
        String intruderUserId = "intruder-user-999";
        when(queueSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession));

        // Act & Assert
        assertThatThrownBy(() -> queueEntryService.callNext(SESSION_ID, intruderUserId))
                .isInstanceOf(AppException.class)
                .hasMessage("You do not have permission to manage this queue.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should throw exception if there is no one waiting in the queue")
    void testCallNext_QueueEmpty() {
        // Arrange
        professional.setId(BARBER_USER_ID);
        when(queueSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession));
        when(queueCacheService.getActiveEntries(SESSION_ID)).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> queueEntryService.callNext(SESSION_ID, BARBER_USER_ID))
                .isInstanceOf(AppException.class)
                .hasMessage("There are no clients waiting in the queue.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should throw not found exception when entry returned by cache does not exist in database")
    void testCallNext_EntryNotFoundInDatabase() {
        // Arrange
        when(queueSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession));
        when(queueCacheService.getActiveEntries(SESSION_ID)).thenReturn(List.of(waitingEntry));
        when(queueEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> queueEntryService.callNext(SESSION_ID, BARBER_USER_ID))
                .isInstanceOf(AppException.class)
                .hasMessage("Entry Id not found")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);

        verify(queueEntryRepository, never()).save(any());
        verify(queueCacheService, never()).evict(anyString());
    }

// ==================== START SERVICE TESTS ====================

    @Test
    @DisplayName("Should start service successfully when client status is WAITING")
    void testStartService_Success() {
        // Arrange
        when(queueEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(waitingEntry));
        when(queueEntryRepository.save(any(QueueEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(queueEntryRepository.findActiveEntriesBySessionId(SESSION_ID))
                .thenReturn(List.of(waitingEntry));

        // Act
        QueueEntryResponseDTO result = queueEntryService.startService(ENTRY_ID, BARBER_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(ENTRY_ID);
        assertThat(result.status()).isEqualTo(QueueEntryStatus.IN_SERVICE);

        verify(queueEntryRepository).save(waitingEntry);

        verify(queueCacheService).evict(SESSION_ID);
        verify(queueEntryRepository).findActiveEntriesBySessionId(SESSION_ID);
    }

    @Test
    @DisplayName("Should throw bad request exception when entry status is neither WAITING nor CALLED")
    void testStartService_InvalidStatus() {
        // Arrange
        waitingEntry.setStatus(QueueEntryStatus.FINISHED);

        when(queueEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(waitingEntry));

        // Act & Assert
        assertThatThrownBy(() -> queueEntryService.startService(ENTRY_ID, BARBER_USER_ID))
                .isInstanceOf(AppException.class)
                .hasMessage("The client must be waiting or called to start the service.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);

        verify(queueEntryRepository, never()).save(any());
        verifyNoInteractions(queueCacheService);
    }

    // ==================== FINISH SERVICE TESTS ====================

    @Test
    @DisplayName("Should finish service successfully and evict cache")
    void testFinishService_Success() {
        // Arrange
        when(queueEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(waitingEntry));
        when(queueEntryRepository.save(any(QueueEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        queueEntryService.finishService(ENTRY_ID, BARBER_USER_ID);

        // Assert
        assertThat(waitingEntry.getStatus()).isEqualTo(QueueEntryStatus.FINISHED);

        verify(queueEntryRepository).save(waitingEntry);
        verify(queueCacheService).evict(SESSION_ID);
    }

    // ==================== CANCEL ENTRY TESTS ====================

    @Test
    @DisplayName("Should allow the client to cancel their own entry")
    void testCancelEntry_ByClient_Success() {
        // Arrange
        when(queueEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(waitingEntry));
        when(queueEntryRepository.save(any(QueueEntry.class))).thenReturn(waitingEntry);

        // Act
        queueEntryService.cancelEntry(ENTRY_ID, CLIENT_USER_ID);

        // Assert
        assertThat(waitingEntry.getStatus()).isEqualTo(QueueEntryStatus.CANCELLED);
        verify(queueCacheService).evict(SESSION_ID);
        verify(queueEntryRepository).save(waitingEntry);
    }

    @Test
    @DisplayName("Should allow the barber to cancel a client's entry")
    void testCancelEntry_ByBarber_Success() {
        // Arrange
        professional.setId(BARBER_USER_ID);
        when(queueEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(waitingEntry));
        when(queueEntryRepository.save(any(QueueEntry.class))).thenReturn(waitingEntry);

        // Act
        queueEntryService.cancelEntry(ENTRY_ID, BARBER_USER_ID);

        // Assert
        assertThat(waitingEntry.getStatus()).isEqualTo(QueueEntryStatus.CANCELLED);
        verify(queueCacheService).evict(SESSION_ID);
    }

    @Test
    @DisplayName("Should throw forbidden exception when user attempting to cancel is neither the barber nor the client")
    void testCancelEntry_Forbidden() {
        // Arrange
        when(queueEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(waitingEntry));
        String intruderUserId = "intruder-user-999";

        // Act & Assert
        assertThatThrownBy(() -> queueEntryService.cancelEntry(ENTRY_ID, intruderUserId))
                .isInstanceOf(AppException.class)
                .hasMessage("You do not have permission to cancel this entry.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);

        verify(queueEntryRepository, never()).save(any());
        verifyNoInteractions(queueCacheService);
    }
}