package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.AuthenticationDTO;
import sodresoftwares.barbearia.dto.LoginResponseDTO;
import sodresoftwares.barbearia.dto.RegisterDTO;
import sodresoftwares.barbearia.dto.RegisterProfessionalDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.infra.security.TokenService;
import sodresoftwares.barbearia.model.Professional;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.repositories.ProfessionalRepository;
import sodresoftwares.barbearia.repositories.UserRepository;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponseDTO login(AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        User loggedUser = (User) Objects.requireNonNull(auth.getPrincipal());

        MDC.put("userId", loggedUser.getId());
        log.info("User authenticated successfully");

        String token = tokenService.generateToken(loggedUser);

        String role = loggedUser.getRole().toString();

        return new LoginResponseDTO(token, role);
    }
}