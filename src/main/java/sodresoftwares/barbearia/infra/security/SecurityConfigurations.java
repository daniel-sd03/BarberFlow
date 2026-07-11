 package sodresoftwares.barbearia.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import sodresoftwares.barbearia.infra.logging.RequestLoggingFilter;

 @Configuration
@EnableWebSecurity
 @RequiredArgsConstructor
public class SecurityConfigurations {

	private final SecurityFilter securityFilter;
	private final RequestLoggingFilter requestLoggingFilter;
	private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
	private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
		return httpSecurity
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authrize -> authrize
						.requestMatchers(HttpMethod.POST, "/auth/barbearia").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/register/professional").permitAll()
						.requestMatchers("/oauth2/**", "/barbearia/oauth2/**").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/error").permitAll()
						.anyRequest().authenticated()
				)
				.oauth2Login(oauth2 -> oauth2
						.successHandler(oAuth2AuthenticationSuccessHandler)
				)
				.exceptionHandling(exception ->
						exception.authenticationEntryPoint(customAuthenticationEntryPoint)
				)
				.addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(requestLoggingFilter, SecurityFilter.class)
				.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
