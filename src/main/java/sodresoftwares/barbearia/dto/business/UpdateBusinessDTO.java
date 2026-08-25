package sodresoftwares.barbearia.dto.business;

import jakarta.validation.constraints.Size;

public record UpdateBusinessDTO(
        @Size(min = 2, max = 150, message = "name must be between 2 and 150 characters.")
        String name
) {}