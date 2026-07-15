package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.JoinQueueDTO;
import sodresoftwares.barbearia.dto.QueueEntryResponseDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.QueueEntry;
import sodresoftwares.barbearia.model.QueueEntryStatus;
import sodresoftwares.barbearia.model.QueueSession;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.repositories.QueueEntryRepository;
import sodresoftwares.barbearia.repositories.QueueSessionRepository;
import sodresoftwares.barbearia.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueEntryService {

    private final QueueEntryRepository queueEntryRepository;
    private final QueueSessionRepository queueSessionRepository;
    private final UserRepository userRepository;

    @Transactional
    public QueueEntryResponseDTO joinQueue(@NonNull JoinQueueDTO dto) {
        QueueSession session = getAndValidateSession(dto.queueSessionId());
        User user = getAndValidateUser(dto.userId());
        validateUserNotInAnyQueue(user.getId());

        QueueEntry entry = QueueEntry.builder()
                .queueSession(session)
                .user(user)
                .serviceName(dto.serviceName())
                .status(QueueEntryStatus.WAITING)
                .build();

        QueueEntry savedEntry = queueEntryRepository.save(entry);
        log.info("User successfully joined queue session {} with entry ID {}", session.getId(), savedEntry.getId());

        return mapToResponseDTO(savedEntry, session.getId());
    }

    public Optional<QueueEntryResponseDTO> findActiveEntryByUserId(String userId) {
        return queueEntryRepository.findByUserIdAndStatusIn(
                userId,
                List.of(QueueEntryStatus.WAITING, QueueEntryStatus.CALLED, QueueEntryStatus.IN_SERVICE)
        ).map(entry -> mapToResponseDTO(entry, entry.getQueueSession().getId()));
    }

    public QueueEntryResponseDTO mapToResponseDTO(QueueEntry entry, String sessionId) {
        List<QueueEntry> activeEntries = queueEntryRepository.findActiveEntriesBySessionId(sessionId);
        int position = 0;

        for (int i = 0; i < activeEntries.size(); i++) {
            if (activeEntries.get(i).getId().equals(entry.getId())) {
                position = i + 1; //+1 because indice start in 0
                break;
            }
        }

        return new QueueEntryResponseDTO(
                entry.getId(),
                position,
                entry.getUser().getId(),
                entry.getUser().getName(),
                entry.getServiceName(),
                entry.getStatus()
        );
    }

    private QueueSession getAndValidateSession(String sessionId) {
        QueueSession session = queueSessionRepository.findById(sessionId)
                .orElseThrow(() -> {
                    log.warn("Join queue failed: session {} not found", sessionId);
                    return new AppException(
                            HttpStatus.NOT_FOUND,
                            "SESSION_NOT_FOUND",
                            "Queue session not found");
                });

        if (!session.getIsActive()) {
            log.warn("Join queue failed: session {} is closed", session.getId());
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "QUEUE_CLOSED",
                    "This queue is currently closed.");
        }

        return session;
    }

    private User getAndValidateUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Join queue failed: user {} not found", userId);
                    return new AppException(
                            HttpStatus.NOT_FOUND,
                            "USER_NOT_FOUND",
                            "User not found");
                });
    }

    private void validateUserNotInAnyQueue(String userId) {
        boolean alreadyInAnyQueue = queueEntryRepository.existsByUserIdAndStatusIn(
                userId,
                List.of(QueueEntryStatus.WAITING, QueueEntryStatus.CALLED, QueueEntryStatus.IN_SERVICE)
        );

        if (alreadyInAnyQueue) {
            log.warn("Join queue failed: user {} is already active in a queue", userId);
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "ALREADY_IN_QUEUE",
                    "You are already waiting in an active queue.");
        }
    }
}