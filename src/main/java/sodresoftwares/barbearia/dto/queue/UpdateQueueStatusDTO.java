package sodresoftwares.barbearia.dto.queue;

import jakarta.validation.constraints.NotNull;

public record UpdateQueueStatusDTO(
        @NotNull(message = "Activate status is required")
        Boolean activate
) {}