package sodresoftwares.barbearia.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sodresoftwares.barbearia.dto.ChangePasswordDTO;
import sodresoftwares.barbearia.dto.RegisterDTO;
import sodresoftwares.barbearia.dto.UpdateUserDTO;
import sodresoftwares.barbearia.dto.UserResponseDTO;
import sodresoftwares.barbearia.infra.security.SecurityFilter;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.services.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = SecurityFilter.class
                )
        },
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JacksonTester<Object> jsonTester;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CacheManager cacheManager;

    private User loggedInUser;
    private RegisterDTO registerDTO;
    private UpdateUserDTO updateDTO;
    private UserResponseDTO responseDTO;


    @BeforeEach
    void setUp() {
        loggedInUser = User.builder()
                .id("user-123")
                .name("Old Name")
                .role(UserRole.USER)
                .build();

        registerDTO = new RegisterDTO(
                "user@test.com",
                "password123",
                "Cliente Teste",
                "11999999999")
        ;

        updateDTO = new UpdateUserDTO(
                "New Name",
                "111111111");

        responseDTO = new UserResponseDTO(
                "user-123",
                "New Name",
                "user@test.com",
                "111111111",
                "USER"
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loggedInUser, null, loggedInUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== GET MY PROFILE TESTS ====================

    @Test
    @DisplayName("GET /users/me -> Should return 200 OK and profile data")
    void testGetMyProfile_Success() throws Exception {
        // Arrange
        when(userService.getMyProfile(any())).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(get("/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.login").value("user@test.com"));
    }

    // ==================== POST REGISTER TESTS ====================

    @Test
    @DisplayName("POST /users -> Should register new user successfully (HTTP 201)")
    void testRegister_Success() throws Exception {
        // Arrange
        User userMock = new User();
        when(userService.registerClient(any(RegisterDTO.class))).thenReturn(userMock);

        // Act & Assert
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(registerDTO).getJson()))
                .andExpect(status().isCreated());

        verify(userService).registerClient(any(RegisterDTO.class));
    }

    @Test
    @DisplayName("POST /users -> Should return 400 when register fields are blank")
    void testRegister_ValidationErrors() throws Exception {
        RegisterDTO invalidDTO = new RegisterDTO("", "", "", "123");

        // Act & Assert
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(invalidDTO).getJson()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    // ==================== PATCH UPDATE USER PROFILE TESTS ====================

    @Test
    @DisplayName("PATCH /users/me -> Should update profile and return 200 OK")
    void testUpdateMyProfile_Success() throws Exception {
        when(userService.updateUserProfile(any(), any())).thenReturn(responseDTO);

        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(updateDTO).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.phone").value("111111111"));
    }

    @Test
    @DisplayName("PATCH /users/me -> Should return 400 Bad Request when DTO has validation errors")
    void testUpdateMyProfile_ValidationError() throws Exception {
        UpdateUserDTO invalidDTO = new UpdateUserDTO("A", "111111111");

        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(invalidDTO).getJson()))
                .andExpect(status().isBadRequest());
    }

    // ==================== PATCH CHANGE PASSWORD TESTS ====================

    @Test
    @DisplayName("PATCH /users/me/password -> Should change password and return 204 No Content")
    void testChangeMyPassword_Success() throws Exception {
        // Arrange
        ChangePasswordDTO dto = new ChangePasswordDTO("oldPass123", "newPass123", "newPass123");
        doNothing().when(userService).changePassword(eq("user-123"), any(ChangePasswordDTO.class));

        // Act & Assert
        mockMvc.perform(patch("/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(dto).getJson()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /users/me/password -> Should return 400 Bad Request when DTO has validation errors")
    void testChangeMyPassword_ValidationError() throws Exception {
        // Arrange:
        ChangePasswordDTO invalidDto = new ChangePasswordDTO("oldPass123", "123", "123");

        // Act & Assert
        mockMvc.perform(patch("/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(invalidDto).getJson()))
                .andExpect(status().isBadRequest());
    }
}