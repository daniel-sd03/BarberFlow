package sodresoftwares.barbearia.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

	private final TokenService tokenService;
	private final HandlerExceptionResolver handlerExceptionResolver;

	@Value("${app.lgpd.current-version}")
	private String currentLgpdVersion;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		var token = this.recoverToken(request);
		if (token != null) {
			var decodedJWT = tokenService.validateAndDecodeToken(token);

			if(decodedJWT != null) {
				String tokenLgpdVersion = decodedJWT.getClaim("lgpd_version").asString();

				boolean isLgpdEndpoint = request.getRequestURI().equals("/api/v1/lgpd-consents");
				if (!isLgpdEndpoint && !currentLgpdVersion.equals(tokenLgpdVersion)) {
					AppException ex = new AppException(
							HttpStatus.FORBIDDEN,
							"LGPD_PENDING",
							"Outdated LGPD terms. Please accept the new version."
					);
					handlerExceptionResolver.resolveException(request, response, null, ex);
					return;
				}

				var authentication = tokenService.getAuthentication(decodedJWT);

				if (authentication != null) {
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			}
		}
		filterChain.doFilter(request, response);
	}

	private String recoverToken(HttpServletRequest request) {
		var authHeader = request.getHeader("Authorization");
		if (authHeader == null)
			return null;
		return authHeader.replace("Bearer ", "");
	}
}
