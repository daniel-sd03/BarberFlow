package sodresoftwares.barbearia.infra.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.repositories.UserRepository;

import java.io.IOException;
import java.util.UUID;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

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
            }
        }
        // Scenario 2: Existing standard user logging in with Google for the first time
        else if (userByEmail != null) {
            finalUser = userByEmail;

            // Link Google account to the existing profile
            finalUser.setGoogleId(googleId);
            userRepository.save(finalUser);
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
        }

        String jwtToken = tokenService.generateToken(finalUser);

        Cookie cookie = new Cookie("TEMP_AUTH_TOKEN", jwtToken);
        cookie.setPath("/");
        cookie.setMaxAge(60);
        // cookie.setSecure(true); // Uncomment in production (HTTPS)

        response.addCookie(cookie);

        String targetUrl = "http://localhost:5173/inicio";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}