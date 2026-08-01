package sodresoftwares.barbearia.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateQueueSessionDTO(
        @Size(min = 2, message = "Prefix must contain at least 2 characters.")
        String prefix,

        @Min(value = 1, message = "Tolerance must be at least 1 minute.")
        Integer toleranceMinutes
) {}