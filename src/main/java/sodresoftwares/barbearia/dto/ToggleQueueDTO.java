package sodresoftwares.barbearia.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleQueueDTO(
        @NotNull
        boolean activate,

        String customPrefix
) {}