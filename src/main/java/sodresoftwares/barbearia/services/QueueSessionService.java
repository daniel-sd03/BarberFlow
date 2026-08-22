package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.*;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.*;
import sodresoftwares.barbearia.repositories.QueueSessionRepository;
import sodresoftwares.barbearia.repositories.TeamMemberRepository;

import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueSessionService {

    private final QueueSessionRepository queueSessionRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final QueueCacheService queueCacheService;

    @Transactional
    public QueueSessionBusinessResponseDTO createQueueSession(String loggedUserId) {
        Business business = getBusinessForOwner(loggedUserId);

        if (queueSessionRepository.existsByBusinessId(business.getId())) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "QUEUE_ALREADY_EXISTS",
                    "This business already has a queue session.");
        }

        String initialPrefix = generateInitialPrefix(business.getName());
        String safeTicketCode = generateUniqueTicketCode(initialPrefix);

        QueueSession newSession = QueueSession.builder()
                .business(business)
                .prefix(initialPrefix)
                .ticketCode(safeTicketCode)
                .isActive(false)
                .build();

        QueueSession savedSession = queueSessionRepository.save(newSession);
        log.info("Queue session created");

        return mapToSessionDTO(savedSession);
    }

    @Transactional
    public QueueSessionBusinessResponseDTO updateQueueStatus(String loggedUserId, boolean activate) {
        Business business = getBusinessForOwner(loggedUserId);

        QueueSession session = queueSessionRepository.findByBusinessId(business.getId())
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "SESSION_NOT_FOUND",
                        "Queue not set up yet."));

        session.setIsActive(activate);
        QueueSession savedSession = queueSessionRepository.save(session);
        log.info("Queue session {}", activate ? "opened" : "closed");

        return mapToSessionDTO(savedSession);
    }

    @Transactional
    public QueueSessionBusinessResponseDTO updateSessionSettings(String loggedUserId, UpdateQueueSessionDTO dto) {
        Business business = getBusinessForOwner(loggedUserId);

        QueueSession session = queueSessionRepository.findByBusinessId(business.getId())
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "SESSION_NOT_FOUND",
                        "Queue not found."
                ));

        boolean wasUpdated = false;

        if (dto.toleranceMinutes() != null) {
            session.setToleranceMinutes(dto.toleranceMinutes());
            wasUpdated = true;
        }

        if (dto.prefix() != null && !dto.prefix().isBlank()) {
            String cleanPrefix = sanitize(dto.prefix());

            if (cleanPrefix.length() < 2) {
                throw new AppException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_PREFIX",
                        "Prefix must contain at least 2 valid alphanumeric characters."
                );
            }

            String newTicketCode = generateUniqueTicketCode(cleanPrefix);
            session.setTicketCode(newTicketCode);
            session.setPrefix(cleanPrefix);
            wasUpdated = true;
        }

        if (wasUpdated) {
            queueSessionRepository.save(session);
            log.info("Queue session settings updated");
        }

        return mapToSessionDTO(session);
    }

    @Transactional
    public QueueSessionBusinessResponseDTO refreshTicketCode(String loggedUserId) {
        Business business = getBusinessForOwner(loggedUserId);

        QueueSession session = queueSessionRepository.findByBusinessId(business.getId())
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "SESSION_NOT_FOUND",
                        "Queue not found."
                ));

        String newTicketCode = generateUniqueTicketCode(session.getPrefix());

        session.setTicketCode(newTicketCode);

        QueueSession savedSession = queueSessionRepository.save(session);

        log.info("Ticket code regenerated");

        return mapToSessionDTO(savedSession);
    }

    public QueueSessionUserResponseDTO getSessionInfoByCode(String ticketCode) {
        QueueSession session = queueSessionRepository.findByTicketCodeWithBusiness(ticketCode.toUpperCase())
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "SESSION_NOT_FOUND",
                        "Queue not found for the Ticket code."
                ));

        int peopleInQueue = queueCacheService.getActiveEntries(session.getId()).size();

        return new QueueSessionUserResponseDTO(
                session.getId(),
                session.getBusiness().getName(),
                peopleInQueue,
                session.getIsActive(),
                session.getToleranceMinutes()
        );
    }

    // Generates prefix based on Business name or defaults to "FILA"
    private String generateInitialPrefix(String businessName) {
        if (businessName != null && !businessName.isBlank()) {
            String sanitized = sanitize(businessName);
            if (sanitized.length() >= 2) {
                return sanitized.substring(0, Math.min(sanitized.length(), 4));
            }
        }
        return "FILA";
    }

    // Sanitizes special characters and spaces to prevent URL breaks
    private String sanitize(String text) {
        return text.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    }

    // Security loop to prevent collision (duplicate codes in the database)
    private String generateUniqueTicketCode(String prefix) {
        String generatedCode;
        boolean codeExists;

        do {
            int randomNumber = ThreadLocalRandom.current().nextInt(1000, 10000);
            String shortCode = String.format("%04d", randomNumber);
            generatedCode = prefix + shortCode;

            codeExists = queueSessionRepository.existsByTicketCode(generatedCode);

            if (codeExists) {
                log.warn("Collision detected for code {}. Generating a new one...", generatedCode);
            }

        } while (codeExists);

        return generatedCode;
    }

    private QueueSessionBusinessResponseDTO mapToSessionDTO(QueueSession session) {
        return new QueueSessionBusinessResponseDTO(
                session.getId(),
                session.getTicketCode(),
                session.getIsActive()
        );
    }

    private TeamMember getTeamMember(String loggedUserId) {
        return teamMemberRepository.findByUserId(loggedUserId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "TEAM_MEMBER_NOT_FOUND",
                        "User is not associated with any team/business."));
    }

    private Business getBusinessForAnyMember(String loggedUserId) {
        return getTeamMember(loggedUserId).getBusiness();
    }

    private Business getBusinessForOwner(String loggedUserId) {
        TeamMember member = getTeamMember(loggedUserId);

        if (member.getRole() != TeamRole.OWNER) {
            throw new AppException(
                    HttpStatus.FORBIDDEN,
                    "ACCESS_DENIED",
                    "Only the business owner can perform this action.");
        }

        return member.getBusiness();
    }
}