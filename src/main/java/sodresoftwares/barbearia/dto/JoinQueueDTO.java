package sodresoftwares.barbearia.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinQueueDTO(
        @NotBlank(message = "Queue session ID is required.")
        String queueSessionId,

        @NotBlank(message = "User ID is required.")
        String userId,

        @NotBlank(message = "Service name cannot be empty.")
        String serviceName
) {}