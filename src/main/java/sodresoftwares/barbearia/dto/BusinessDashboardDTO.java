package sodresoftwares.barbearia.dto;

import sodresoftwares.barbearia.model.TeamRole;

import java.util.List;

public record BusinessDashboardDTO(
        String businessId,
        String businessName,
        String loggedMemberId,
        TeamRole loggedMemberRole,
        String sessionId,
        String ticketCode,
        boolean isActive,
        Integer toleranceMinutes,
        List<QueueEntryResponseDTO> activeQueue,
        List<TeamMemberDTO> team,
        List<TeamInviteResponseDTO> pendingInvites
) {}