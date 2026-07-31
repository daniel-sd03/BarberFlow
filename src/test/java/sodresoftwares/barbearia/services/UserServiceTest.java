package sodresoftwares.barbearia.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import sodresoftwares.barbearia.dto.UpdateUserDTO;
import sodresoftwares.barbearia.dto.UserResponseDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.repositories.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private final String USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .name("Old Name")
                .login("user@test.com")
                .phone("000000000")
                .role(UserRole.USER)
                .build();
    }

    // ==================== GET MY PROFILE TESTS ====================

    @Test
    @DisplayName("Should return user profile when user exists")
    void testGetMyProfile_Success() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        // Act
        UserResponseDTO result = userService.getMyProfile(USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(USER_ID);
        assertThat(result.name()).isEqualTo("Old Name");
        assertThat(result.login()).isEqualTo("user@test.com");

        verify(userRepository).findById(USER_ID);
    }

    @Test
    @DisplayName("Should throw not found exception when user does not exist on get profile")
    void testGetMyProfile_UserNotFound() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getMyProfile(USER_ID))
                .isInstanceOf(AppException.class)
                .hasMessage("User not found.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ==================== UPDATE USER PROFILE TESTS ====================

    @Test
    @DisplayName("Should update both name and phone when valid DTO is provided")
    void testUpdateUserProfile_Success_UpdateAllFields() {
        // Arrange
        UpdateUserDTO updateDTO = new UpdateUserDTO("New Name", "111111111");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponseDTO result = userService.updateUserProfile(USER_ID, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.phone()).isEqualTo("111111111");

        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should update ONLY the name when phone is null in DTO")
    void testUpdateUserProfile_Success_UpdateOnlyName() {
        // Arrange
        UpdateUserDTO updateDTO = new UpdateUserDTO("New Name", null);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponseDTO result = userService.updateUserProfile(USER_ID, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.phone()).isEqualTo("000000000");

        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should update ONLY the phone when name is null or blank in DTO")
    void testUpdateUserProfile_Success_UpdateOnlyPhone() {
        // Arrange
        UpdateUserDTO updateDTO = new UpdateUserDTO("   ", "222222222");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponseDTO result = userService.updateUserProfile(USER_ID, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Old Name");
        assertThat(result.phone()).isEqualTo("222222222");

        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw not found exception when user does not exist")
    void testUpdateUserProfile_UserNotFound() {
        // Arrange
        UpdateUserDTO updateDTO = new UpdateUserDTO("New Name", "111111111");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserProfile(USER_ID, updateDTO))
                .isInstanceOf(AppException.class)
                .hasMessage("User not found.")
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);

        verify(userRepository, never()).save(any());
    }
}