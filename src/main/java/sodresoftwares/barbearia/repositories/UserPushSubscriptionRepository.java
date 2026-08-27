package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sodresoftwares.barbearia.model.UserPushSubscription;
import java.util.List;

public interface UserPushSubscriptionRepository extends JpaRepository<UserPushSubscription, String> {

    boolean existsByEndpoint(String endpoint);

    List<UserPushSubscription> findAllByUserId(String userId);
}