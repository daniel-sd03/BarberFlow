package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.ProfessionalDashboardDTO;
import sodresoftwares.barbearia.dto.QueueEntryResponseDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.Professional;
import sodresoftwares.barbearia.model.QueueEntry;
import sodresoftwares.barbearia.model.QueueSession;
import sodresoftwares.barbearia.repositories.ProfessionalRepository;
import sodresoftwares.barbearia.repositories.QueueEntryRepository;
import sodresoftwares.barbearia.repositories.QueueSessionRepository;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueSessionService {

    private final QueueSessionRepository queueSessionRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final ProfessionalRepository professionalRepository;

    @Transactional
    public QueueSession toggleQueue(String loggedUserId, boolean activate, String customPrefix) {

        QueueSession session = queueSessionRepository.findByProfessionalId(loggedUserId)
                .orElseGet(() -> {
                    Professional professional = professionalRepository.findByUserId(loggedUserId)
                            .orElseThrow(() -> {
                                log.warn("Toggle queue failed: professional not found");
                                return new AppException(
                                        HttpStatus.NOT_FOUND,
                                        "PROFESSIONAL_NOT_FOUND",
                                        "Professional not found");});

                    String finalPrefix = determinePrefix(customPrefix, professional.getBusinessName());
                    String safeTicketCode = generateUniqueTicketCode(finalPrefix);

                    return QueueSession.builder()
                            .professional(professional)
                            .ticketCode(safeTicketCode)
                            .isActive(activate)
                            .build();
                });

        session.setIsActive(activate);
        return queueSessionRepository.save(session);
    }

    public ProfessionalDashboardDTO getDashboardData(String loggedUserId) {
        QueueSession session = queueSessionRepository.findByProfessionalId(loggedUserId)
                .orElseThrow(() -> {
                    log.warn("Dashboard fetch failed: session not found for professional");
                    return new AppException(
                            HttpStatus.NOT_FOUND,
                            "SESSION_NOT_FOUND",
                            "No queue session found for this professional.");
                });

        List<QueueEntry> activeEntries = queueEntryRepository.findActiveEntriesBySessionId(session.getId());

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

    // Priority rule: Custom prefix > Business name > "FILA"
    private String determinePrefix(String customPrefix, String businessName) {
        if (customPrefix != null && !customPrefix.isBlank()) {
            String sanitized = sanitize(customPrefix);
            if (sanitized.length() >= 3) {
                return sanitized.substring(0, Math.min(sanitized.length(), 5));
            }
        }

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
}