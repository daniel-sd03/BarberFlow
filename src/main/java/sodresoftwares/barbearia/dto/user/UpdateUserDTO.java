package sodresoftwares.barbearia.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateUserDTO(
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters.")
        String name,

        @Size(max = 20, message = "Phone number is too long.")
        String phone
) {}