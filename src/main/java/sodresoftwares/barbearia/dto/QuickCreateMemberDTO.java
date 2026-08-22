package sodresoftwares.barbearia.dto;

import jakarta.validation.constraints.NotBlank;

public record QuickCreateMemberDTO(
        @NotBlank(message = "Name is required")
        String name
) {}