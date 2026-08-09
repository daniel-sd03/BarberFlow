package sodresoftwares.barbearia.infra.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.model.LgpdConsent;
import sodresoftwares.barbearia.repositories.LgpdConsentRepository;
import sodresoftwares.barbearia.repositories.UserRepository;
import sodresoftwares.barbearia.services.LgpdConsentService;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    final private TokenService tokenService;
    final private UserRepository userRepository;
    private final LgpdConsentRepository lgpdConsentRepository;
    private final LgpdConsentService lgpdConsentService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String googleId = oAuth2User.getAttribute("sub");

        User userByGoogleId = userRepository.findByGoogleId(googleId);
        User userByEmail = (User) userRepository.findByLogin(email);

        User finalUser;

        // Scenario 1: Existing Google Auth user
        if (userByGoogleId != null) {
            finalUser = userByGoogleId;

            // Sync email if it was updated in Google's dashboard
            if (!finalUser.getLogin().equals(email)) {
                finalUser.setLogin(email);
                userRepository.save(finalUser);
                log.info("Google account email synchronized");
            }
        }
        // Scenario 2: Existing standard user logging in with Google for the first time
        else if (userByEmail != null) {
            finalUser = userByEmail;

            // Link Google account to the existing profile
            finalUser.setGoogleId(googleId);
            userRepository.save(finalUser);

            log.info("Google account linked successfully");
        }
        // Scenario 3: Brand new user
        else {
            finalUser = User.builder()
                    .login(email)
                    .googleId(googleId)
                    .password(UUID.randomUUID().toString())
                    .role(UserRole.USER)
                    .build();
            userRepository.save(finalUser);
            lgpdConsentService.registerConsentForNewUser(finalUser, request);

            log.info("New user registered via Google OAuth2");
        }

        String lgpdLastAcceptedVersion = lgpdConsentRepository.findFirstByUserIdOrderByCreatedAtDesc(finalUser.getId())
                .map(LgpdConsent::getTermVersion)
                .orElse(null);

        String jwtToken = tokenService.generateToken(finalUser, lgpdLastAcceptedVersion);

        Cookie tokenCookie = new Cookie("TEMP_AUTH_TOKEN", jwtToken);
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(60);
        // tokenCookie.setSecure(true); // Descomentar em produção (HTTPS)
        response.addCookie(tokenCookie);

        Cookie roleCookie = new Cookie("TEMP_ROLE", finalUser.getRole().name());
        roleCookie.setPath("/");
        roleCookie.setMaxAge(60);
        // roleCookie.setSecure(true); // Descomentar em produção (HTTPS)
        response.addCookie(roleCookie);

        String targetUrl = "http://localhost:5173/inicio";
        log.info("OAuth2 authentication completed successfully for user {}", finalUser.getId());
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}