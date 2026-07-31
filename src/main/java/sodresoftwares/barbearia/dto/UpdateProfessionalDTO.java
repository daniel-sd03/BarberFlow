package sodresoftwares.barbearia.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfessionalDTO(
        @Size(min = 2, max = 150, message = "Business name must be between 2 and 150 characters.")
        String businessName
) {}