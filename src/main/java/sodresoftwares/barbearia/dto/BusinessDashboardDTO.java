package sodresoftwares.barbearia.dto;

import java.util.List;

public record BusinessDashboardDTO(
        String businessId,
        String businessName,
        String loggedMemberId,
        String loggedMemberRole,
        String sessionId,
        String ticketCode,
        boolean isActive,
        Integer toleranceMinutes,
        List<QueueEntryResponseDTO> activeQueue
) {}