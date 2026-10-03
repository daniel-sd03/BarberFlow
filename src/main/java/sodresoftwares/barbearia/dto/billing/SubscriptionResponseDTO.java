package sodresoftwares.barbearia.dto.billing;

import sodresoftwares.barbearia.model.billing.SubscriptionStatus;
import java.time.LocalDate;

public record SubscriptionResponseDTO(
        SubscriptionStatus status,
        String planName,
        LocalDate currentPeriodEnd,
        String paymentLink
) {}