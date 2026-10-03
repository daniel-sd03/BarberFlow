package sodresoftwares.barbearia.services.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sodresoftwares.barbearia.model.billing.Subscription;
import sodresoftwares.barbearia.model.billing.SubscriptionHistory;
import sodresoftwares.barbearia.model.billing.SubscriptionStatus;
import sodresoftwares.barbearia.repositories.billing.SubscriptionHistoryRepository;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionHistoryService Tests")
class SubscriptionHistoryServiceTest {

    @Mock
    private SubscriptionHistoryRepository historyRepository;

    @InjectMocks
    private SubscriptionHistoryService historyService;

    private Subscription mockSubscription;

    private final String REASON = "TEST_REASON";

    @BeforeEach
    void setUp() {
        mockSubscription = Subscription.builder()
                .id("sub-123")
                .status(SubscriptionStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should save history when status actually changes")
    void testLogStatusChange_Success() {
        // Act
        historyService.logStatusChange(mockSubscription, SubscriptionStatus.SUSPENDED, REASON);

        // Assert
        verify(historyRepository).save(argThat(history ->
                history.getSubscription().getId().equals("sub-123") &&
                        history.getPreviousStatus().equals(SubscriptionStatus.ACTIVE) &&
                        history.getNewStatus().equals(SubscriptionStatus.SUSPENDED) &&
                        history.getReason().equals(REASON)
        ));
    }

    @Test
    @DisplayName("Should ignore and NOT save history if status is the same")
    void testLogStatusChange_SameStatus() {
        // Act
        historyService.logStatusChange(mockSubscription, SubscriptionStatus.ACTIVE, REASON);

        // Assert
        verify(historyRepository, never()).save(org.mockito.ArgumentMatchers.any(SubscriptionHistory.class));
    }
}