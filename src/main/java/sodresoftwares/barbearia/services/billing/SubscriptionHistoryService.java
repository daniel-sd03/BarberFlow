package sodresoftwares.barbearia.services.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.model.billing.Subscription;
import sodresoftwares.barbearia.model.billing.SubscriptionHistory;
import sodresoftwares.barbearia.model.billing.SubscriptionStatus;
import sodresoftwares.barbearia.repositories.billing.SubscriptionHistoryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionHistoryService {

    private final SubscriptionHistoryRepository historyRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void logStatusChange(Subscription subscription, SubscriptionStatus newStatus, String reason) {

        if (subscription.getStatus() == newStatus) {
            return;
        }

        SubscriptionHistory history = SubscriptionHistory.builder()
                .subscription(subscription)
                .previousStatus(subscription.getStatus())
                .newStatus(newStatus)
                .reason(reason)
                .build();

        historyRepository.save(history);
    }
}