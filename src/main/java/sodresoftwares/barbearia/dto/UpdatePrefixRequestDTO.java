package sodresoftwares.barbearia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePrefixRequestDTO(
        @NotBlank(message = "Prefix is required")
        @Size(min = 2, max = 6, message = "Prefix must be between 2 and 6 characters")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Prefix must contain only alphanumeric characters")
        String prefix
) {}