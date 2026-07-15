package sodresoftwares.barbearia.dto;

import java.util.List;

public record ProfessionalDashboardDTO(
        String sessionId,
        String businessName,
        String ticketCode,
        boolean isActive,
        List<QueueEntryResponseDTO> activeQueue
) {}