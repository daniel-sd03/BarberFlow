package sodresoftwares.barbearia.dto;

import java.time.Instant;

public record ErrorResponseDTO(
        Instant timestamp,
        Integer status,
        String error,
        String errorCode,
        String message,
        String path
) {}

