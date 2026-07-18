package sodresoftwares.barbearia.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateQueueStatusDTO(
        @NotNull(message = "Activate status is required")
        Boolean activate
) {}