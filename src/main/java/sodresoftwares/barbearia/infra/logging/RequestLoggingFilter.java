package sodresoftwares.barbearia.infra.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sodresoftwares.barbearia.model.user.User;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/favicon.ico"
    );

    private static final String ANONYMOUS_USER = "ANONYMOUS";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        boolean ignored = EXCLUDED_PATHS.stream()
                .anyMatch(uri::startsWith);

        if (ignored) {
            filterChain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();

        try {
            MDC.put("userId", getCurrentUserId());
            MDC.put("request", request.getMethod() + " " + request.getRequestURI());

            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;

            log.info(
                    "Request completed - status={} - duration={}ms",
                    response.getStatus(),
                    duration
            );

            MDC.clear();
        }
    }

    private String getCurrentUserId() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {

            return ANONYMOUS_USER;
        }

        return user.getId();
    }
}