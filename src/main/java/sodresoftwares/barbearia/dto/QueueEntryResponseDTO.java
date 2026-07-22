package sodresoftwares.barbearia.dto;

import sodresoftwares.barbearia.model.QueueEntryStatus;

import java.time.Instant;

public record QueueEntryResponseDTO(
        String id,
        Integer position,
        String userId,
        String clientName,
        String serviceName,
        QueueEntryStatus status,
        Instant calledAt
) {}