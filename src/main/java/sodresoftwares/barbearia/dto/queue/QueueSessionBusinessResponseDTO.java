package sodresoftwares.barbearia.dto.queue;

public record QueueSessionBusinessResponseDTO(
        String id,
        String ticketCode,
        boolean isActive
) {}
