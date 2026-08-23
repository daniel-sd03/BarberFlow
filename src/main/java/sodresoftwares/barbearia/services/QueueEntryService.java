package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.JoinQueueDTO;
import sodresoftwares.barbearia.dto.QueueEntryResponseDTO;
import sodresoftwares.barbearia.dto.UserQueueStatusDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.mappers.QueueMapper;
import sodresoftwares.barbearia.model.*;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.repositories.QueueEntryRepository;
import sodresoftwares.barbearia.repositories.QueueSessionRepository;
import sodresoftwares.barbearia.repositories.TeamMemberRepository;
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
    private final TeamMemberRepository teamMemberRepository;
    private final QueueCacheService queueCacheService;
    private final QueueMapper queueMapper;
    private final QueueNotificationService queueNotificationService;

    public UserQueueStatusDTO getUserQueueStatus(String userId) {
        QueueEntryResponseDTO active = findActiveEntryByUserId(userId).orElse(null);
        QueueEntryResponseDTO latest = null;

        if (active == null) {
            latest = findLatestEntryByUserId(userId).orElse(null);
        }

        return new UserQueueStatusDTO(active, latest);
    }

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
        User clientUser = userRepository.findById(loggedUserId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User not found"
                ));
        validateUserNotInAnyQueue(loggedUserId);

        QueueEntry entry = QueueEntry.builder()
                .queueSession(session)
                .user(clientUser)
                .serviceName(dto.serviceName())
                .status(QueueEntryStatus.WAITING)
                .build();

        QueueEntry savedEntry = queueEntryRepository.save(entry);
        log.info("User joined queue successfully");

        queueCacheService.evict(dto.queueSessionId());

        List<QueueEntry> activeEntries = queueEntryRepository.findActiveEntriesBySessionId(dto.queueSessionId());
        queueNotificationService.notifyQueueUpdate(dto.queueSessionId());

        return queueMapper.toSingleDto(savedEntry, activeEntries);
    }

    @Transactional
    public QueueEntryResponseDTO callNext(String sessionId, String loggedUserId, String actionMemberId) {
        QueueSession session = getAndValidateSession(sessionId);
        TeamMember memberCalling = getValidActionMember(session, loggedUserId, actionMemberId);
        validateMemberNotAlreadyAttending(memberCalling);
        List<QueueEntry> activeEntries = queueCacheService.getActiveEntries(sessionId);

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
        nextInLine.setServedByMember(memberCalling);

        QueueEntry savedEntry = queueEntryRepository.save(nextInLine);
        log.info("Next client called");

        queueCacheService.evict(sessionId);
        List<QueueEntry> updatedActiveEntries = queueEntryRepository.findActiveEntriesBySessionId(sessionId);
        queueNotificationService.notifyQueueUpdate(sessionId);

        return queueMapper.toSingleDto(savedEntry, updatedActiveEntries);
    }

    @Transactional
    public QueueEntryResponseDTO requeueEntry(String entryId, String loggedUserId) {
        QueueEntry entry = getEntryById(entryId);
        String sessionId = entry.getQueueSession().getId();

        validatePermissionToManageEntry(entry, loggedUserId);
        validateStatusForRequeue(entry);

        entry.setMissedCalls(entry.getMissedCalls() + 1);
        entry.setStatus(QueueEntryStatus.WAITING);
        entry.setServedByMember(null);

        List<QueueEntry> activeEntries = queueCacheService.getActiveEntries(sessionId);

        List<QueueEntry> waitingList = activeEntries.stream()
                .filter(e -> e.getStatus() == QueueEntryStatus.WAITING && !e.getId().equals(entryId))
                .toList();

        if (!waitingList.isEmpty()) {
            QueueEntry newFirst = waitingList.get(0);
            entry.setJoinedAt(newFirst.getJoinedAt().plusSeconds(1));
        }

        QueueEntry savedEntry = queueEntryRepository.save(entry);
        log.info("Client returned to queue");

        queueCacheService.evict(sessionId);
        List<QueueEntry> newActiveEntries = queueEntryRepository.findActiveEntriesBySessionId(sessionId);
        queueNotificationService.notifyQueueUpdate(sessionId);
        return queueMapper.toSingleDto(savedEntry, newActiveEntries);
    }

    @Transactional
    public QueueEntryResponseDTO startService(String entryId, String loggedUserId) {
        QueueEntry entry = getEntryById(entryId);
        String sessionId = entry.getQueueSession().getId();

        validatePermissionToManageEntry(entry, loggedUserId);
        validateStatusForStart(entry);

        entry.setStatus(QueueEntryStatus.IN_SERVICE);
        QueueEntry savedEntry = queueEntryRepository.save(entry);
        log.info("Service started");

        queueCacheService.evict(sessionId);
        List<QueueEntry> newActiveEntries = queueEntryRepository.findActiveEntriesBySessionId(sessionId);
        queueNotificationService.notifyQueueUpdate(sessionId);

        return queueMapper.toSingleDto(savedEntry, newActiveEntries);
    }

    @Transactional
    public void finishService(String entryId, String loggedUserId) {
        QueueEntry entry = getEntryById(entryId);

        validatePermissionToManageEntry(entry, loggedUserId);
        validateStatusForFinish(entry);

        entry.setStatus(QueueEntryStatus.FINISHED);
        queueEntryRepository.save(entry);
        log.info("Service finished");

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
        log.info("Queue entry cancelled");

        queueCacheService.evict(entry.getQueueSession().getId());
        queueNotificationService.notifyQueueUpdate(entry.getQueueSession().getId());
    }

    // ==========================================
    // DATA FETCHING & CORE VALIDATIONS
    // ==========================================

    private QueueEntry getEntryById(String entryId) {
        return queueEntryRepository.findByIdWithFullGraph(entryId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "ENTRY_NOT_FOUND",
                        "Entry Id not found"));
    }

    private QueueSession getAndValidateSession(String sessionId) {
        QueueSession session = queueSessionRepository.findByIdWithBusinessAndUser(sessionId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "SESSION_NOT_FOUND",
                        "Queue session not found"));

        if (!session.getIsActive()) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "QUEUE_CLOSED",
                    "This queue is currently closed.");
        }
        return session;
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
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "ALREADY_IN_QUEUE",
                    "You are already waiting in an active queue.");
        }
    }

    private TeamMember getValidActionMember(QueueSession session, String loggedUserId, String actionMemberId) {

        TeamMember actionMember = teamMemberRepository.findById(actionMemberId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "MEMBER_NOT_FOUND",
                        "Team member not found."));

        if (!actionMember.getBusiness().getId().equals(session.getBusiness().getId())) {
            throw new AppException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "This member does not belong to this business.");
        }

        boolean isOwnAccount = actionMember.getUser() != null &&
                actionMember.getUser().getId().equals(loggedUserId);

        boolean isOwnerDevice = teamMemberRepository.existsByUserIdAndBusinessIdAndRole(
                loggedUserId,
                session.getBusiness().getId(),
                TeamRole.OWNER
        );

        if (!isOwnAccount && !isOwnerDevice) {
            log.warn("Security alert: User {} attempted to act as member {} without permission", loggedUserId, actionMemberId);
            throw new AppException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "You do not have permission to perform actions for this member.");
        }

        return actionMember;
    }

    private void validatePermissionToManageEntry(QueueEntry entry, String loggedUserId) {
        String businessId = entry.getQueueSession().getBusiness().getId();

        TeamMember loggedMember = teamMemberRepository.findByUserIdAndBusinessId(loggedUserId, businessId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.FORBIDDEN,
                        "FORBIDDEN",
                        "You do not belong to the team of this business."));

        if (loggedMember.getRole() == TeamRole.OWNER) {
            return;
        }

        TeamMember servedBy = entry.getServedByMember();
        if (servedBy != null && !servedBy.getId().equals(loggedMember.getId())) {
            log.warn("Security alert: User {} attempted to modify an entry served by member {}", loggedUserId, servedBy.getId());
            throw new AppException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "You can only manage the clients you are currently serving."
            );
        }
    }
    private void validateMemberNotAlreadyAttending(TeamMember member) {
        boolean isBusy = queueEntryRepository.existsByServedByMemberIdAndStatusIn(
                member.getId(),
                List.of(QueueEntryStatus.CALLED, QueueEntryStatus.IN_SERVICE)
        );

        if (isBusy) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "MEMBER_BUSY",
                    "You are already attending a client. Finish the current service or cancel before calling the next one."
            );
        }
    }

    private void validateCancelPermission(QueueEntry entry, String loggedUserId) {
        boolean isTeamMember = teamMemberRepository.existsByUserIdAndBusinessId(
                loggedUserId,
                entry.getQueueSession().getBusiness().getId()
        );
        boolean isTheClient = entry.getUser().getId().equals(loggedUserId);

        if (!isTeamMember && !isTheClient) {
            log.warn("Security alert: User {} attempted to cancel entry {} without permission", loggedUserId, entry.getId());
            throw new AppException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "You do not have permission to cancel this entry.");
        }
    }

    private void validateStatusForStart(QueueEntry entry) {
        if (entry.getStatus() != QueueEntryStatus.CALLED) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATUS",
                    "The client must be called to start the service.");
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