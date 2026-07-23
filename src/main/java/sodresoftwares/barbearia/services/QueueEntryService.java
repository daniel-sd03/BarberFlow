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
import sodresoftwares.barbearia.mappers.QueueMapper;
import sodresoftwares.barbearia.model.QueueEntry;
import sodresoftwares.barbearia.model.QueueEntryStatus;
import sodresoftwares.barbearia.model.QueueSession;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.repositories.QueueEntryRepository;
import sodresoftwares.barbearia.repositories.QueueSessionRepository;
import sodresoftwares.barbearia.repositories.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final QueueCacheService queueCacheService;
    private final QueueMapper queueMapper;
    private final QueueNotificationService queueNotificationService;

    public Optional<QueueEntryResponseDTO> findActiveEntryByUserId(String userId) {
        return queueEntryRepository.findByUserIdAndStatusIn(
                userId,
                List.of(
                        QueueEntryStatus.WAITING,
                        QueueEntryStatus.CALLED,
                        QueueEntryStatus.IN_SERVICE)
        ).map(entry -> {
            List<QueueEntry> activeEntries = queueCacheService.getActiveEntries(entry.getQueueSession().getId());
            return queueMapper.toSingleDto(entry, activeEntries);
        });
    }

    public Optional<QueueEntryResponseDTO> findLatestEntryByUserId(String userId) {
        return queueEntryRepository.findFirstByUserIdOrderByJoinedAtDesc(userId)
                .filter(entry -> {
                    Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
                    return entry.getJoinedAt().isAfter(twentyFourHoursAgo);
                })
                .map(entry -> queueMapper.toSingleDto(entry, List.of(entry)));
    }

    @Transactional
    public QueueEntryResponseDTO joinQueue(@NonNull JoinQueueDTO dto, String loggedUserId) {
        QueueSession session = getAndValidateSession(dto.queueSessionId());
        User user = getAndValidateUser(loggedUserId);
        validateUserNotInAnyQueue(user.getId());

        QueueEntry entry = QueueEntry.builder()
                .queueSession(session)
                .user(user)
                .serviceName(dto.serviceName())
                .status(QueueEntryStatus.WAITING)
                .build();

        QueueEntry savedEntry = queueEntryRepository.save(entry);
        log.info("User successfully joined queue session {} with entry ID {}", session.getId(), savedEntry.getId());

        queueCacheService.evict(dto.queueSessionId());

        List<QueueEntry> activeEntries = queueEntryRepository.findActiveEntriesBySessionId(dto.queueSessionId());
        queueNotificationService.notifyQueueUpdate(dto.queueSessionId());

        return queueMapper.toSingleDto(savedEntry, activeEntries);
    }

    @Transactional
    public QueueEntryResponseDTO callNext(String sessionId, String loggedUserId) {
        QueueSession session = getAndValidateSession(sessionId);
        validateProfessionalOwnership(session, loggedUserId);

        List<QueueEntry> activeEntries = queueCacheService.getActiveEntries(sessionId);
        validateChairIsAvailableForCall(activeEntries);

        String nextEntryId = activeEntries.stream()
                .filter(e -> e.getStatus() == QueueEntryStatus.WAITING)
                .map(QueueEntry::getId)
                .findFirst()
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "QUEUE_EMPTY",
                        "There are no clients waiting in the queue."));

        QueueEntry nextInLine = getEntryById(nextEntryId);
        nextInLine.setStatus(QueueEntryStatus.CALLED);
        nextInLine.setCalledAt(Instant.now());

        QueueEntry savedEntry = queueEntryRepository.save(nextInLine);

        queueCacheService.evict(sessionId);
        List<QueueEntry> updatedActiveEntries = queueEntryRepository.findActiveEntriesBySessionId(sessionId);
        queueNotificationService.notifyQueueUpdate(sessionId);

        return queueMapper.toSingleDto(savedEntry, updatedActiveEntries);
    }

    @Transactional
    public QueueEntryResponseDTO requeueEntry(String entryId, String loggedUserId) {
        QueueEntry entry = getEntryById(entryId);
        String sessionId = entry.getQueueSession().getId();

        validateProfessionalOwnership(entry.getQueueSession(), loggedUserId);
        validateStatusForRequeue(entry);

        // Increment missed calls and reset state back to WAITING
        entry.setMissedCalls(entry.getMissedCalls() + 1);
        entry.setStatus(QueueEntryStatus.WAITING);

        QueueEntry savedEntry = queueEntryRepository.save(entry);

        queueCacheService.evict(sessionId);
        List<QueueEntry> updatedActiveEntries = queueEntryRepository.findActiveEntriesBySessionId(sessionId);
        queueNotificationService.notifyQueueUpdate(sessionId);

        return queueMapper.toSingleDto(savedEntry, updatedActiveEntries);
    }

    @Transactional
    public QueueEntryResponseDTO startService(String entryId, String loggedUserId) {
        QueueEntry entry = getEntryById(entryId);
        String sessionId = entry.getQueueSession().getId();

        validateProfessionalOwnership(entry.getQueueSession(), loggedUserId);
        validateChairIsAvailableForStart(sessionId);
        validateStatusForStart(entry);

        entry.setStatus(QueueEntryStatus.IN_SERVICE);
        QueueEntry savedEntry = queueEntryRepository.save(entry);

        queueCacheService.evict(sessionId);
        List<QueueEntry> newActiveEntries = queueEntryRepository.findActiveEntriesBySessionId(sessionId);
        queueNotificationService.notifyQueueUpdate(sessionId);

        return queueMapper.toSingleDto(savedEntry, newActiveEntries);
    }

    @Transactional
    public void finishService(String entryId, String loggedUserId) {
        QueueEntry entry = getEntryById(entryId);

        validateProfessionalOwnership(entry.getQueueSession(), loggedUserId);
        validateStatusForFinish(entry);

        entry.setStatus(QueueEntryStatus.FINISHED);
        queueEntryRepository.save(entry);

        queueCacheService.evict(entry.getQueueSession().getId());
        queueNotificationService.notifyQueueUpdate(entry.getQueueSession().getId());
    }

    @Transactional
    public void cancelEntry(String entryId, String loggedUserId) {
        QueueEntry entry = getEntryById(entryId);

        validateCancelPermission(entry, loggedUserId);
        validateStatusForCancellation(entry, loggedUserId);

        entry.setStatus(QueueEntryStatus.CANCELLED);
        queueEntryRepository.save(entry);

        queueCacheService.evict(entry.getQueueSession().getId());
        queueNotificationService.notifyQueueUpdate(entry.getQueueSession().getId());
    }

    // ==========================================
    // DATA FETCHING & CORE VALIDATIONS
    // ==========================================

    private QueueEntry getEntryById(String entryId) {
        return queueEntryRepository.findById(entryId)
                .orElseThrow(() -> {
                    log.warn("Entry lookup failed: entry {} not found", entryId);
                    return new AppException(
                            HttpStatus.NOT_FOUND,
                            "ENTRY_NOT_FOUND",
                            "Entry Id not found");
                });
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

    // ==========================================
    // BUSINESS RULES & STATE MACHINE VALIDATIONS
    // ==========================================

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

    private void validateProfessionalOwnership(QueueSession session, String loggedUserId) {
        if (!session.getProfessional().getUser().getId().equals(loggedUserId)) {
            log.warn("Security alert: User {} attempted to modify session {} without permission", loggedUserId, session.getId());
            throw new AppException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "You do not have permission to manage this queue.");
        }
    }

    private void validateCancelPermission(QueueEntry entry, String loggedUserId) {
        boolean isTheBarber = entry.getQueueSession().getProfessional().getUser().getId().equals(loggedUserId);
        boolean isTheClient = entry.getUser().getId().equals(loggedUserId);

        if (!isTheBarber && !isTheClient) {
            log.warn("Security alert: User {} attempted to cancel entry {} without permission", loggedUserId, entry.getId());
            throw new AppException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "You do not have permission to cancel this entry.");
        }
    }

    private void validateChairIsAvailableForCall(List<QueueEntry> activeEntries) {
        boolean isSomeoneBeingAttended = activeEntries.stream()
                .anyMatch(e -> e.getStatus() == QueueEntryStatus.CALLED || e.getStatus() == QueueEntryStatus.IN_SERVICE);

        if (isSomeoneBeingAttended) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "CHAIR_OCCUPIED",
                    "Finish the current service or cancel the called client before calling the next one."
            );
        }
    }

    private void validateChairIsAvailableForStart(String sessionId) {
        List<QueueEntry> activeEntries = queueCacheService.getActiveEntries(sessionId);
        boolean isSomeoneInService = activeEntries.stream()
                .anyMatch(e -> e.getStatus() == QueueEntryStatus.IN_SERVICE);

        if (isSomeoneInService) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "CHAIR_OCCUPIED",
                    "There is already a client in service. Please finish their service first.");
        }
    }

    private void validateStatusForStart(QueueEntry entry) {
        if (entry.getStatus() != QueueEntryStatus.CALLED && entry.getStatus() != QueueEntryStatus.WAITING) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATUS",
                    "The client must be waiting or called to start the service.");
        }
    }

    private void validateStatusForFinish(QueueEntry entry) {
        if (entry.getStatus() != QueueEntryStatus.IN_SERVICE) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATUS",
                    "You can only finish a service that is currently in progress (IN_SERVICE).");
        }
    }

    private void validateStatusForCancellation(QueueEntry entry, String loggedUserId) {
        if (entry.getStatus() == QueueEntryStatus.CANCELLED || entry.getStatus() == QueueEntryStatus.FINISHED) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATUS",
                    "This entry is already cancelled or finished.");
        }

        boolean isTheClient = entry.getUser().getId().equals(loggedUserId);
        if (isTheClient && entry.getStatus() == QueueEntryStatus.IN_SERVICE) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "SERVICE_IN_PROGRESS",
                    "You cannot cancel your spot while the service is in progress."
            );
        }
    }

    private void validateStatusForRequeue(QueueEntry entry) {
        if (entry.getStatus() != QueueEntryStatus.CALLED) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATUS",
                    "Only clients with CALLED status can be returned to the queue.");
        }
    }
}