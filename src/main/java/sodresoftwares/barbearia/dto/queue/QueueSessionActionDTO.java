package sodresoftwares.barbearia.dto.queue;

import jakarta.validation.constraints.NotBlank;

public record QueueSessionActionDTO(
        @NotBlank(message = "Session ID is required")
        String sessionId
) {}
