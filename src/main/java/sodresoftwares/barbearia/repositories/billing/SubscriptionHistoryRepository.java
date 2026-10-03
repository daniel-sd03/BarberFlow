package sodresoftwares.barbearia.repositories.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import sodresoftwares.barbearia.model.billing.SubscriptionHistory;

public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, String> {
}