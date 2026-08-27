package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import sodresoftwares.barbearia.model.UserPushSubscription;
import sodresoftwares.barbearia.repositories.UserPushSubscriptionRepository;

import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
@Async
public class PushNotificationDispatcher {

    private final UserPushSubscriptionRepository pushRepository;
    private final WebPushService webPushService;

    public void notifyClientTurn(String userId, String barberName) {
        String title = "É a sua vez! ✂️";
        String message = "Pode ir para a cadeira, o profissional " + barberName + " está te aguardando.";

        notifyUser(userId, title, message);
    }

    public void notifyUser(String userId, String title, String message) {
        List<UserPushSubscription> subscriptions = pushRepository.findAllByUserId(userId);

        if (subscriptions.isEmpty()) {
            log.info("No active push subscriptions found");
            return;
        }

        String jsonPayload = """
                {
                    "title": "%s",
                    "body": "%s"
                }
                """.formatted(title, message);

        for (UserPushSubscription sub : subscriptions) {
            webPushService.sendPushNotification(
                    sub.getEndpoint(),
                    sub.getP256dh(),
                    sub.getAuth(),
                    jsonPayload
            );
        }
    }
}