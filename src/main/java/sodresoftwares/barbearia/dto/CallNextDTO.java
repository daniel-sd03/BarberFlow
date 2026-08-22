package sodresoftwares.barbearia.dto;

import jakarta.validation.constraints.NotBlank;

public record CallNextDTO(
        @NotBlank(message = "O action member ID is required.")
        String actionMemberId
) {}