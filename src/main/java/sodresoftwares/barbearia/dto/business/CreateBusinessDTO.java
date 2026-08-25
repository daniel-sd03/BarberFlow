package sodresoftwares.barbearia.dto.business;

import jakarta.validation.constraints.NotBlank;

public record CreateBusinessDTO(
        @NotBlank(message = "Name is required ")
        String name
) {}