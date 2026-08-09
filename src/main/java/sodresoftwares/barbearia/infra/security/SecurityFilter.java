package sodresoftwares.barbearia.infra.security;

import java.io.IOException;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.repositories.UserRepository;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

	private final TokenService tokenService;
	private final UserRepository userRepository;
	private final HandlerExceptionResolver handlerExceptionResolver;

	@Value("${app.lgpd.current-version}")
	private String currentLgpdVersion;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		var token = this.recoverToken(request);
		if (token != null) {
			var login = tokenService.validateToken(token);
			if(login != null) {
				String tokenLgpdVersion = tokenService.getLgpdVersionFromToken(token);
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
				UserDetails user = userRepository.findByLogin(login);
				if(user != null) {
					var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
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
