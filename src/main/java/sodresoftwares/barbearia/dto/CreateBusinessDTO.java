package sodresoftwares.barbearia.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBusinessDTO(
        @NotBlank(message = "Name is required ")
        String name
) {}