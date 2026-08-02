package sodresoftwares.barbearia.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import sodresoftwares.barbearia.dto.ProfessionalResponseDTO;
import sodresoftwares.barbearia.dto.RegisterDTO;
import sodresoftwares.barbearia.dto.RegisterProfessionalDTO;
import sodresoftwares.barbearia.dto.UpdateProfessionalDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.Professional;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.repositories.ProfessionalRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfessionalService Tests")
class ProfessionalServiceTest {

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProfessionalService professionalService;
    private RegisterProfessionalDTO registerProfDTO;

    private Professional testProfessional;
    private final String USER_ID = "user-123";
    private final String PROF_ID = "prof-123";

    @BeforeEach
    void setUp() {
        User testUser = User.builder()
                .id(USER_ID)
                .login("barbeiro@test.com")
                .name("Barbeiro Zé")
                .role(UserRole.PROFESSIONAL)
                .build();

        testProfessional = Professional.builder()
                .id(PROF_ID)
                .user(testUser)
                .businessName("Old Business Name")
                .isActive(true)
                .build();

        registerProfDTO = new RegisterProfessionalDTO(
                "barbeiro@test.com",
                "password123",
                "Barbeiro Zé",
                "11999999999",
                "Barbearia do Zé"
        );
    }

    // ==================== GET MY PROFESSIONAL PROFILE TESTS ====================

    @Test
    @DisplayName("Should return professional profile including user data when it exists")
    void testGetMyProfessionalProfile_Success() {
        // Arrange
        when(professionalRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testProfessional));

        // Act
        ProfessionalResponseDTO result = professionalService.getMyProfessionalProfile(USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(PROF_ID);
        assertThat(result.businessName()).isEqualTo("Old Business Name");

        assertThat(result.user()).isNotNull();
        assertThat(result.user().id()).isEqualTo(USER_ID);
        assertThat(result.user().name()).isEqualTo("Barbeiro Zé");

        verify(professionalRepository).findByUserId(USER_ID);
    }

    @Test
    @DisplayName("Should throw not found exception when professional profile does not exist on get")
    void testGetMyProfessionalProfile_NotFound() {
        // Arrange
        when(professionalRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> professionalService.getMyProfessionalProfile(USER_ID))
                .isInstanceOf(AppException.class)
                .hasMessage("Professional profile not found for this user.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ==================== REGISTER PROFESSIONAL TESTS ====================

    @Test
    @DisplayName("Should register new professional successfully")
    void testRegisterProfessional_Successful() {
        // Arrange:
        when(userService.createUser(any(RegisterDTO.class), eq(UserRole.PROFESSIONAL)))
                .thenReturn(testProfessional.getUser());

        // Act
        professionalService.registerProfessional(registerProfDTO);

        // Assert
        verify(userService).createUser(any(RegisterDTO.class), eq(UserRole.PROFESSIONAL));
        verify(professionalRepository).save(argThat(professional ->
                professional.getUser().getId().equals(USER_ID) &&
                        professional.getBusinessName().equals("Barbearia do Zé") &&
                        professional.getIsActive().equals(true)
        ));
    }

    @Test
    @DisplayName("Should throw exception when trying to register an existing professional")
    void testRegisterProfessional_UserAlreadyExists() {
        // Arrange: Se o UserService lançar exceção, o fluxo deve ser interrompido
        when(userService.createUser(any(RegisterDTO.class), eq(UserRole.PROFESSIONAL)))
                .thenThrow(new AppException(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "User already exists"));

        // Act & Assert
        assertThatThrownBy(() -> professionalService.registerProfessional(registerProfDTO))
                .isInstanceOf(AppException.class)
                .hasMessage("User already exists")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);

        verify(userService).createUser(any(RegisterDTO.class), eq(UserRole.PROFESSIONAL));
        verify(professionalRepository, never()).save(any(Professional.class));
    }

    // ==================== UPDATE PROFESSIONAL PROFILE TESTS ====================

    @Test
    @DisplayName("Should update business name when valid DTO is provided")
    void testUpdateProfessionalProfile_Success() {
        // Arrange
        UpdateProfessionalDTO updateDTO = new UpdateProfessionalDTO("New Business Name");

        when(professionalRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testProfessional));
        when(professionalRepository.save(any(Professional.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProfessionalResponseDTO result = professionalService.updateProfessionalProfile(USER_ID, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.businessName()).isEqualTo("New Business Name");
        assertThat(result.isActive()).isTrue();

        verify(professionalRepository).save(testProfessional);
    }

    @Test
    @DisplayName("Should NOT update business name if DTO value is blank")
    void testUpdateProfessionalProfile_Success_BlankNameIgnored() {
        // Arrange
        UpdateProfessionalDTO updateDTO = new UpdateProfessionalDTO("   ");

        when(professionalRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testProfessional));
        when(professionalRepository.save(any(Professional.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProfessionalResponseDTO result = professionalService.updateProfessionalProfile(USER_ID, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.businessName()).isEqualTo("Old Business Name");

        verify(professionalRepository).save(testProfessional);
    }

    @Test
    @DisplayName("Should throw not found exception when professional profile does not exist for the user")
    void testUpdateProfessionalProfile_ProfessionalNotFound() {
        // Arrange
        UpdateProfessionalDTO updateDTO = new UpdateProfessionalDTO("New Business Name");
        when(professionalRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> professionalService.updateProfessionalProfile(USER_ID, updateDTO))
                .isInstanceOf(AppException.class)
                .hasMessage("Professional profile not found for this user.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);

        verify(professionalRepository, never()).save(any());
    }
}