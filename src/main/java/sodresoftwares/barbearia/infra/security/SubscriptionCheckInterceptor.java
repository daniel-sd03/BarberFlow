package sodresoftwares.barbearia.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import sodresoftwares.barbearia.model.billing.SubscriptionStatus;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.repositories.billing.SubscriptionRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionCheckInterceptor implements HandlerInterceptor {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return true;
        }

        if (!(authentication.getPrincipal() instanceof User authenticatedUser)) {
            log.error("Security Anomaly: Expected Principal of type User, but got: {}",
                    authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getName() : "null");
            return true;
        }

        String userId = authenticatedUser.getId();


        SubscriptionStatus status = subscriptionRepository.findSubscriptionStatusByUserId(userId).orElse(null);

        if (status == SubscriptionStatus.SUSPENDED || status == SubscriptionStatus.CANCELED) {
            log.warn("Access denied (402). Subscription is {}.", status);

            response.setStatus(HttpStatus.PAYMENT_REQUIRED.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                    "error": "PAYMENT_REQUIRED", 
                    "message": "Your subscription is suspended or canceled. Please settle your payment to access the business features."
                }
            """);
            return false;
        }

        return true;
    }
}