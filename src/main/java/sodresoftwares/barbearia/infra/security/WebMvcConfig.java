package sodresoftwares.barbearia.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import sodresoftwares.barbearia.infra.security.SubscriptionCheckInterceptor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SubscriptionCheckInterceptor subscriptionCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(subscriptionCheckInterceptor)
                .addPathPatterns(
                        "/queue-sessions/**",
                        "/queue-entries/**",
                        "/team-members/**"
                );
    }
}