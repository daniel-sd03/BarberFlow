package sodresoftwares.barbearia.dto.queue;

public record QueueSessionUserResponseDTO(
        String sessionId,
        String businessName,
        int peopleInQueue,
        boolean isActive,
        Integer toleranceMinutes
) {}