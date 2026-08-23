package sodresoftwares.barbearia.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import sodresoftwares.barbearia.dto.BusinessResponseDTO;
import sodresoftwares.barbearia.dto.CreateBusinessDTO;
import sodresoftwares.barbearia.dto.UpdateBusinessDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.Business;
import sodresoftwares.barbearia.model.TeamMember;
import sodresoftwares.barbearia.model.TeamRole;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.repositories.BusinessRepository;
import sodresoftwares.barbearia.repositories.TeamMemberRepository;
import sodresoftwares.barbearia.repositories.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessService Tests")
class BusinessServiceTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BusinessService businessService;

    private CreateBusinessDTO createBusinessDTO;
    private Business testBusiness;
    private User testUser;

    private final String USER_ID = "user-123";
    private final String BUSINESS_ID = "biz-123";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .login("barbeiro@test.com")
                .name("Barbeiro Zé")
                .role(UserRole.PROFESSIONAL)
                .build();

        testBusiness = Business.builder()
                .id(BUSINESS_ID)
                .user(testUser)
                .name("Old Business Name")
                .isActive(true)
                .build();

        createBusinessDTO = new CreateBusinessDTO("Barbearia do Zé");
    }

    // ==================== GET MY BUSINESS PROFILE TESTS ====================

    @Test
    @DisplayName("Should return business profile including user data when it exists")
    void testGetMyBusinessProfile_Success() {
        // Arrange
        when(businessRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testBusiness));

        // Act
        BusinessResponseDTO result = businessService.getMyBusinessProfile(USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(BUSINESS_ID);
        assertThat(result.name()).isEqualTo("Old Business Name");

        assertThat(result.user()).isNotNull();
        assertThat(result.user().id()).isEqualTo(USER_ID);
        assertThat(result.user().name()).isEqualTo("Barbeiro Zé");

        verify(businessRepository).findByUserId(USER_ID);
    }

    @Test
    @DisplayName("Should throw not found exception when business profile does not exist on get")
    void testGetMyBusinessProfile_NotFound() {
        // Arrange
        when(businessRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> businessService.getMyBusinessProfile(USER_ID))
                .isInstanceOf(AppException.class)
                .hasMessage("Business profile not found for this user.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ==================== CREATE BUSINESS TESTS ====================

    @Test
    @DisplayName("Should create new business and owner team member successfully")
    void testCreateBusiness_Successful() {
        // Arrange
        when(businessRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(businessRepository.save(any(Business.class))).thenReturn(testBusiness);

        // Act
        businessService.createBusiness(USER_ID, createBusinessDTO);

        // Assert
        verify(businessRepository).existsByUserId(USER_ID);
        verify(userRepository).findById(USER_ID);

        verify(businessRepository).save(argThat(business ->
                business.getUser().getId().equals(USER_ID) &&
                        business.getName().equals("Barbearia do Zé")
        ));

        verify(teamMemberRepository).save(argThat(member ->
                member.getBusiness().getId().equals(BUSINESS_ID) &&
                        member.getUser().getId().equals(USER_ID) &&
                        member.getRole().equals(TeamRole.OWNER) &&
                        member.getName().equals("Barbeiro Zé")
        ));
    }

    @Test
    @DisplayName("Should throw exception when trying to create a business for a user that already has one")
    void testCreateBusiness_UserAlreadyHasBusiness() {
        // Arrange
        when(businessRepository.existsByUserId(USER_ID)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> businessService.createBusiness(USER_ID, createBusinessDTO))
                .isInstanceOf(AppException.class)
                .hasMessage("This user already owns a registered business.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository, never()).findById(any());
        verify(businessRepository, never()).save(any(Business.class));
        verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("Should throw exception when user is not found during business creation")
    void testCreateBusiness_UserNotFound() {
        // Arrange
        when(businessRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> businessService.createBusiness(USER_ID, createBusinessDTO))
                .isInstanceOf(AppException.class)
                .hasMessage("User not found.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);

        verify(businessRepository, never()).save(any(Business.class));
        verify(teamMemberRepository, never()).save(any(TeamMember.class));
    }

    // ==================== UPDATE BUSINESS PROFILE TESTS ====================

    @Test
    @DisplayName("Should update business name when valid DTO is provided")
    void testUpdateBusinessProfile_Success() {
        // Arrange
        UpdateBusinessDTO updateDTO = new UpdateBusinessDTO("New Business Name");

        when(businessRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testBusiness));
        when(businessRepository.save(any(Business.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BusinessResponseDTO result = businessService.updateBusinessProfile(USER_ID, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("New Business Name");

        verify(businessRepository).save(testBusiness);
    }

    @Test
    @DisplayName("Should NOT update business name if DTO value is blank")
    void testUpdateBusinessProfile_Success_BlankNameIgnored() {
        // Arrange
        UpdateBusinessDTO updateDTO = new UpdateBusinessDTO("   ");

        when(businessRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testBusiness));
        when(businessRepository.save(any(Business.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BusinessResponseDTO result = businessService.updateBusinessProfile(USER_ID, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Old Business Name");

        verify(businessRepository).save(testBusiness);
    }

    @Test
    @DisplayName("Should throw not found exception when business profile does not exist for the user")
    void testUpdateBusinessProfile_BusinessNotFound() {
        // Arrange
        UpdateBusinessDTO updateDTO = new UpdateBusinessDTO("New Business Name");
        when(businessRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> businessService.updateBusinessProfile(USER_ID, updateDTO))
                .isInstanceOf(AppException.class)
                .hasMessage("Business profile not found for this user.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);

        verify(businessRepository, never()).save(any());
    }
}