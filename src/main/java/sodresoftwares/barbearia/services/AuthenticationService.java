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

    public String login(AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        User loggedUser = (User) Objects.requireNonNull(auth.getPrincipal());

        MDC.put("userId", loggedUser.getId());
        log.info("User authenticated successfully");

        return tokenService.generateToken((User) Objects.requireNonNull(loggedUser));
    }

    @Transactional
    public User register(RegisterDTO data) {
        if (this.userRepository.existsByLogin(data.login())) {
            log.warn("Registration failed: user already exists");
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "USER_ALREADY_EXISTS",
                    "User already exists");
        }

        String encryptedPassword = passwordEncoder.encode(data.password());

        User newUser = User.builder()
                .login(data.login())
                .password(encryptedPassword)
                .role(data.role())
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("User registered with role {}", savedUser.getRole());

        return savedUser;
    }

    @Transactional
    public void registerProfessional(RegisterProfessionalDTO data) {
        RegisterDTO baseUserDTO = new RegisterDTO(data.login(), data.password(), UserRole.PROFESSIONAL);

        User savedUser = this.register(baseUserDTO);

        Professional newProfessional = Professional.builder()
                .user(savedUser)
                .businessName(data.businessName())
                .contactPhone(data.contactPhone())
                .isActive(true)
                .build();

        professionalRepository.save(newProfessional);

        log.info("Professional registered successfully: {}", data.businessName());
    }
}