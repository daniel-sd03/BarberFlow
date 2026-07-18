package sodresoftwares.barbearia.services;

import org.h2.engine.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authService;

    private User testUser;
    private User testProfUser;
    private AuthenticationDTO authDTO;
    private RegisterDTO registerDTO;
    private RegisterProfessionalDTO registerProfDTO;


    private final String TEST_LOGIN = "user@test.com";
    private final String RAW_PASSWORD = "password123";
    private final String ENCODED_PASSWORD = "$2a$10$encodedPasswordHash...";
    private final String TEST_NAME = "Fulano da Silva";
    private final String TEST_PHONE = "11999999999";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-123")
                .login(TEST_LOGIN)
                .password(ENCODED_PASSWORD)
                .name(TEST_NAME)
                .phone(TEST_PHONE)
                .role(UserRole.USER)
                .build();

        testProfUser = User.builder()
                .id("prof-123")
                .login(TEST_LOGIN)
                .password(ENCODED_PASSWORD)
                .name(TEST_NAME)
                .phone(TEST_PHONE)
                .role(UserRole.PROFESSIONAL)
                .build();

        authDTO = new AuthenticationDTO(TEST_LOGIN, RAW_PASSWORD);
        registerDTO = new RegisterDTO(TEST_LOGIN, RAW_PASSWORD, TEST_NAME, TEST_PHONE);
        registerProfDTO = new RegisterProfessionalDTO(TEST_LOGIN, RAW_PASSWORD, TEST_NAME, TEST_PHONE, "Barbearia do Zé");
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Should login user successfully and return JWT token")
    void testLogin_Successful() {
        // Arrange
        UsernamePasswordAuthenticationToken authenticatedToken = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticatedToken);
        when(tokenService.generateToken(testUser))
                .thenReturn("valid-jwt-token");

        // Act
        LoginResponseDTO result = authService.login(authDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("valid-jwt-token");
        assertThat(result.role()).isEqualTo(UserRole.USER.toString());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService).generateToken(testUser);
    }

    @Test
    @DisplayName("Should throw exception when login credentials are invalid")
    void testLogin_InvalidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(authDTO))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService, never()).generateToken(any());
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("Should register new user successfully")
    void testRegister_Successful() {
        // Arrange
        when(userRepository.existsByLogin(TEST_LOGIN)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        authService.register(registerDTO);

        // Assert
        verify(userRepository).existsByLogin(TEST_LOGIN);
        verify(passwordEncoder).encode(RAW_PASSWORD);
        verify(userRepository).save(argThat(user ->
                user.getLogin().equals(TEST_LOGIN) &&
                        user.getPassword().equals(ENCODED_PASSWORD) &&
                        user.getName().equals(TEST_NAME) &&
                        user.getPhone().equals(TEST_PHONE) &&
                        user.getRole().equals(UserRole.USER)
        ));
    }

    @Test
    @DisplayName("Should throw exception when trying to register an existing login")
    void testRegister_UserAlreadyExists() {
        // Arrange
        when(userRepository.existsByLogin(TEST_LOGIN)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerDTO))
                .isInstanceOf(AppException.class)
                .hasMessage("User already exists")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository).existsByLogin(TEST_LOGIN);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== REGISTER PROFESSIONAL TESTS ====================

    @Test
    @DisplayName("Should register new professional successfully")
    void testRegisterProfessional_Successful() {
        // Arrange
        when(userRepository.existsByLogin(TEST_LOGIN)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(testProfUser);

        // Act
        authService.registerProfessional(registerProfDTO);

        // Assert
        verify(userRepository).existsByLogin(TEST_LOGIN);
        verify(passwordEncoder).encode(RAW_PASSWORD);

        verify(userRepository).save(argThat(user ->
                user.getLogin().equals(TEST_LOGIN) &&
                        user.getPassword().equals(ENCODED_PASSWORD) &&
                        user.getName().equals(TEST_NAME) &&
                        user.getPhone().equals(TEST_PHONE) &&
                        user.getRole().equals(UserRole.PROFESSIONAL)
        ));

        verify(professionalRepository).save(argThat(professional ->
                professional.getUser().getId().equals("prof-123") &&
                        professional.getBusinessName().equals("Barbearia do Zé") &&
                        professional.getIsActive().equals(true)
        ));
    }

    @Test
    @DisplayName("Should throw exception when trying to register an existing professional")
    void testRegisterProfessional_UserAlreadyExists() {
        // Arrange
        when(userRepository.existsByLogin(TEST_LOGIN)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.registerProfessional(registerProfDTO))
                .isInstanceOf(AppException.class)
                .hasMessage("User already exists")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository).existsByLogin(TEST_LOGIN);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(professionalRepository, never()).save(any(Professional.class));
    }
}