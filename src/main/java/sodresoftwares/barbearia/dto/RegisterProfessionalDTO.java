package sodresoftwares.barbearia.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterProfessionalDTO(
        @NotBlank String login,
        @NotBlank String password,
        @NotBlank String businessName,
        String contactPhone
) {}