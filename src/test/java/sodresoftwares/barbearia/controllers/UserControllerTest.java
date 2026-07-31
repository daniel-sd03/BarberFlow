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
import sodresoftwares.barbearia.dto.UpdateUserDTO;
import sodresoftwares.barbearia.dto.UserResponseDTO;
import sodresoftwares.barbearia.infra.security.SecurityFilter;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.services.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private JacksonTester<UpdateUserDTO> updateUserJson;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CacheManager cacheManager;

    private User loggedInUser;
    private UpdateUserDTO updateDTO;
    private UserResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        loggedInUser = User.builder()
                .id("user-123")
                .name("Old Name")
                .role(UserRole.USER)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loggedInUser, null, loggedInUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        updateDTO = new UpdateUserDTO("New Name", "111111111");

        responseDTO = new UserResponseDTO(
                "user-123",
                "New Name",
                "user@test.com",
                "111111111",
                "USER"
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== GET MY PROFILE TESTS ====================

    @Test
    @DisplayName("GET /api/users/me -> Should return 200 OK and profile data")
    void testGetMyProfile_Success() throws Exception {
        // Arrange
        when(userService.getMyProfile(any())).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(get("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.login").value("user@test.com"));
    }

    // ==================== UPDATE USER PROFILE TESTS ====================

    @Test
    @DisplayName("PATCH /api/users/me -> Should update profile and return 200 OK")
    void testUpdateMyProfile_Success() throws Exception {
        when(userService.updateUserProfile(any(), any())).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateUserJson.write(updateDTO).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.phone").value("111111111"));
    }

    @Test
    @DisplayName("PATCH /api/users/me -> Should return 400 Bad Request when DTO has validation errors")
    void testUpdateMyProfile_ValidationError() throws Exception {
        UpdateUserDTO invalidDTO = new UpdateUserDTO("A", "111111111");

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateUserJson.write(invalidDTO).getJson()))
                .andExpect(status().isBadRequest());
    }
}