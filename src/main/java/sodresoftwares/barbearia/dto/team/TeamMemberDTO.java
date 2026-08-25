package sodresoftwares.barbearia.dto.team;

import sodresoftwares.barbearia.model.TeamRole;

public record TeamMemberDTO(
        String id,
        String name,
        TeamRole role
) {}
