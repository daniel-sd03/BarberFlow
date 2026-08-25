package sodresoftwares.barbearia.dto.queue;

public record UserQueueStatusDTO(
        QueueEntryResponseDTO activeEntry,
        QueueEntryResponseDTO latestHistoricalEntry
) {}
