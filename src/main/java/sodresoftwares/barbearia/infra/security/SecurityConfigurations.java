package sodresoftwares.barbearia.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
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
                .authorizeHttpRequests(authorize -> authorize

                        // ==========================================
                        // 1. PUBLIC ENDPOINTS (No authentication required)
                        // ==========================================
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register/professional").permitAll()
                        .requestMatchers("/oauth2/**", "/barbearia/oauth2/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()

                        // ==========================================
                        // 2. PROFESSIONAL ONLY ENDPOINTS
                        // ==========================================
                        // Queue Session Management
                        .requestMatchers(HttpMethod.POST, "/api/queue-sessions").hasRole("PROFESSIONAL")
                        .requestMatchers(HttpMethod.PATCH, "/api/queue-sessions/status").hasRole("PROFESSIONAL")
                        .requestMatchers(HttpMethod.GET, "/api/queue-sessions/dashboard").hasRole("PROFESSIONAL")

                        // Queue Entry Management (Call, Start, Finish)
                        .requestMatchers(HttpMethod.POST, "/api/queue-entries/session/*/call-next").hasRole("PROFESSIONAL")
                        .requestMatchers(HttpMethod.POST, "/api/queue-entries/*/start").hasRole("PROFESSIONAL")
                        .requestMatchers(HttpMethod.POST, "/api/queue-entries/*/finish").hasRole("PROFESSIONAL")
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
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("PROFESSIONAL")
                .role("PROFESSIONAL").implies("USER")
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
