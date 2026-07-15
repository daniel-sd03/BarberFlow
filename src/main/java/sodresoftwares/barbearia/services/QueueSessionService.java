package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.Professional;
import sodresoftwares.barbearia.model.QueueSession;
import sodresoftwares.barbearia.repositories.ProfessionalRepository;
import sodresoftwares.barbearia.repositories.QueueSessionRepository;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueSessionService {

    private final QueueSessionRepository queueSessionRepository;
    private final ProfessionalRepository professionalRepository;

    @Transactional
    public QueueSession toggleQueue(String professionalId, boolean activate, String customPrefix) {

        QueueSession session = queueSessionRepository.findByProfessionalId(professionalId)
                .orElseGet(() -> {
                    Professional professional = professionalRepository.findById(professionalId)
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