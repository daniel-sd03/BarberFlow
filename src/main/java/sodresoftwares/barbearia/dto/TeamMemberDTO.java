package sodresoftwares.barbearia.dto;

import sodresoftwares.barbearia.model.TeamRole;

public record TeamMemberDTO(
        String id,
        String name,
        TeamRole role
) {}
