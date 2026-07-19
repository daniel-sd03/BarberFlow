package sodresoftwares.barbearia.dto;

public record QueueSessionUserResponseDTO(
        String sessionId,
        String businessName,
        int peopleInQueue,
        boolean isActive
) {}