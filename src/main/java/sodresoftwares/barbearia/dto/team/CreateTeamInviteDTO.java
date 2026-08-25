package sodresoftwares.barbearia.dto.team;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateTeamInviteDTO(
        @NotBlank(message = "E-mail is required")
        @Email(message = "Invalid e-mail format")
        String email
) {}