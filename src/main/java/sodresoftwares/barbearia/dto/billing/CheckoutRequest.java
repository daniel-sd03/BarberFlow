package sodresoftwares.barbearia.dto.billing;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(
        @NotBlank(message = "The business ID is required.")
        String businessId,

        @NotBlank(message = "The plan code is required.")
        String planCode
) {}