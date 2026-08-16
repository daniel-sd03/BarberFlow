package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.AuthenticationDTO;
import sodresoftwares.barbearia.dto.TokenResponseDTO;
import sodresoftwares.barbearia.infra.security.TokenService;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.LgpdConsent;
import sodresoftwares.barbearia.repositories.LgpdConsentRepository;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final LgpdConsentRepository lgpdConsentRepository;

    public TokenResponseDTO login(AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        User loggedUser = (User) Objects.requireNonNull(auth.getPrincipal());
        try {
            MDC.put("userId", loggedUser.getId());
            log.info("User authenticated successfully");

            String lgpdLastAcceptedVersion = lgpdConsentRepository.findFirstByUserIdOrderByCreatedAtDesc(loggedUser.getId())
                    .map(LgpdConsent::getTermVersion)
                    .orElse(null);

            String token = tokenService.generateToken(loggedUser, lgpdLastAcceptedVersion);
            String role = loggedUser.getRole().toString();

            return new TokenResponseDTO(token, role);
        } finally {
            MDC.remove("userId");
        }
    }
}