package sodresoftwares.barbearia.dto;

public record UserQueueStatusDTO(
        QueueEntryResponseDTO activeEntry,
        QueueEntryResponseDTO latestHistoricalEntry
) {}
