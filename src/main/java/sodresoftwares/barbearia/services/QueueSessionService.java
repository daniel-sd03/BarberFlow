package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.ProfessionalDashboardDTO;
import sodresoftwares.barbearia.dto.QueueEntryResponseDTO;
import sodresoftwares.barbearia.dto.QueueSessionProfResponseDTO;
import sodresoftwares.barbearia.dto.QueueSessionUserResponseDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.Professional;
import sodresoftwares.barbearia.model.QueueEntry;
import sodresoftwares.barbearia.model.QueueSession;
import sodresoftwares.barbearia.repositories.ProfessionalRepository;
import sodresoftwares.barbearia.repositories.QueueSessionRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueSessionService {

    private final QueueSessionRepository queueSessionRepository;
    private final ProfessionalRepository professionalRepository;
    private final QueueCacheService queueCacheService;

    @Transactional
    public QueueSessionProfResponseDTO createQueueSession(String loggedUserId) {
        if (queueSessionRepository.existsByProfessionalUserId(loggedUserId)) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "QUEUE_ALREADY_EXISTS",
                    "This professional already has a queue session.");
        }

        Professional professional = professionalRepository.findByUserId(loggedUserId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "PROFESSIONAL_NOT_FOUND",
                        "Professional not found"));

        String finalPrefix = determinePrefix(professional.getBusinessName());
        String safeTicketCode = generateUniqueTicketCode(finalPrefix);

        QueueSession newSession = QueueSession.builder()
                .professional(professional)
                .ticketCode(safeTicketCode)
                .isActive(false)
                .build();

        QueueSession savedSession = queueSessionRepository.save(newSession);

        return mapToSessionDTO(savedSession);
    }

    @Transactional
    public QueueSessionProfResponseDTO updateQueueStatus(String loggedUserId, boolean activate) {
        QueueSession session = queueSessionRepository.findByProfessionalUserId(loggedUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Queue not set up yet."));

        session.setIsActive(activate);
        QueueSession savedSession = queueSessionRepository.save(session);

        return mapToSessionDTO(savedSession);
    }

    public ProfessionalDashboardDTO getDashboardData(String loggedUserId) {
        Optional<QueueSession> sessionOpt = queueSessionRepository.findByProfessionalUserId(loggedUserId);

        if (sessionOpt.isEmpty()) {
            Professional professional = professionalRepository.findByUserId(loggedUserId)
                    .orElseThrow(() -> new AppException(
                            HttpStatus.NOT_FOUND,
                            "PROFESSIONAL_NOT_FOUND",
                            "Professional not found"));

            return new ProfessionalDashboardDTO(
                    null,
                    professional.getBusinessName(),
                    null,
                    false,
                    List.of()
            );
        }

        QueueSession session = sessionOpt.get();
        List<QueueEntry> activeEntries = queueCacheService.getActiveEntries(session.getId());

        List<QueueEntryResponseDTO> queueDTOs = activeEntries.stream()
                .map(entry -> new QueueEntryResponseDTO(
                        entry.getId(),
                        activeEntries.indexOf(entry) + 1,
                        entry.getUser().getId(),
                        entry.getUser().getName(),
                        entry.getServiceName(),
                        entry.getStatus()
                ))
                .toList();

        return new ProfessionalDashboardDTO(
                session.getId(),
                session.getProfessional().getBusinessName(),
                session.getTicketCode(),
                session.getIsActive(),
                queueDTOs
        );
    }

    public QueueSessionUserResponseDTO getSessionInfoByCode(String ticketCode) {
        QueueSession session = queueSessionRepository.findByTicketCode(ticketCode.toUpperCase())
                .orElseThrow(() -> {
                    log.warn("Session search failed: code {} not found", ticketCode);
                    return new AppException(
                            HttpStatus.NOT_FOUND,
                            "SESSION_NOT_FOUND",
                            "Queue not found for the Ticket code.");

                });

        int peopleInQueue = queueCacheService.getActiveEntries(session.getId()).size();

        return new QueueSessionUserResponseDTO(
                session.getId(),
                session.getProfessional().getBusinessName(),
                peopleInQueue,
                session.getIsActive()
        );
    }

    // Generates prefix based on Business name or defaults to "FILA"
    private String determinePrefix(String businessName) {
        if (businessName != null && !businessName.isBlank()) {
            String sanitized = sanitize(businessName);
            if (sanitized.length() >= 3) {
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
            String shortCode = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            generatedCode = prefix + shortCode;

            codeExists = queueSessionRepository.existsByTicketCode(generatedCode);

            if (codeExists) {
                log.warn("Collision detected for code {}. Generating a new one...", generatedCode);
            }

        } while (codeExists);

        return generatedCode;
    }

    private QueueSessionProfResponseDTO mapToSessionDTO(QueueSession session) {
        return new QueueSessionProfResponseDTO(
                session.getId(),
                session.getTicketCode(),
                session.getIsActive()
        );
    }
}