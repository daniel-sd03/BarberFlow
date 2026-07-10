package sodresoftwares.barbearia.infra.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.barbearia.infra.security.SecurityFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = GlobalExceptionHandlerTest.TestController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityFilter.class
        ),
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
@Import({
        GlobalExceptionHandlerTest.TestController.class,
        GlobalExceptionHandler.class})
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JacksonTester<Object> jsonTester;

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @PostMapping("/app-exception")
        public void throwAppException() {
            throw new AppException(HttpStatus.CONFLICT, "TEST_CONFLICT_CODE", "Test generic application error");
        }

        @PostMapping("/bad-credentials")
        public void throwBadCredentialsException() {
            throw new BadCredentialsException("Invalid credentials");
        }

        @PostMapping("/validation-error")
        public void throwValidationException(@Valid @RequestBody TestDTO dto) {
            // This will trigger MethodArgumentNotValidException
        }

        @PostMapping("/malformed-json")
        public void throwMalformedJson(@RequestBody Object dummy) {
            // This will be triggered by sending invalid JSON
        }

        @GetMapping("/missing-param")
        public void throwMissingParam(@RequestParam String requiredParam) {
            // This will trigger MissingServletRequestParameterException if param is missing
        }

        @GetMapping("/type-mismatch/{id}")
        public void throwTypeMismatch(@PathVariable Long id) {
            // This will trigger MethodArgumentTypeMismatchException if id is not a number
        }

        @GetMapping("/entity-not-found")
        public void throwEntityNotFoundException() {
            throw new EntityNotFoundException("User not found in database");
        }

        @PostMapping("/generic-exception")
        public void throwGenericException() {
            throw new RuntimeException("Test generic error");
        }
    }

    record TestDTO(@NotBlank String name) {}

    @Test
    @DisplayName("Should handle AppException with custom errorCode and status")
    void shouldHandleAppException() throws Exception {
        mockMvc.perform(post("/test/app-exception"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.errorCode").value("TEST_CONFLICT_CODE"))
                .andExpect(jsonPath("$.message").value("Test generic application error"))
                .andExpect(jsonPath("$.path").value("/test/app-exception"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle BadCredentialsException with 401 Unauthorized")
    void shouldHandleBadCredentialsException() throws Exception {
        mockMvc.perform(post("/test/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password."))
                .andExpect(jsonPath("$.path").value("/test/bad-credentials"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with validation errors")
    void shouldHandleValidationException() throws Exception {
        TestDTO invalidDTO = new TestDTO("");

        mockMvc.perform(post("/test/validation-error")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(invalidDTO).getJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("name: must not be blank"))
                .andExpect(jsonPath("$.path").value("/test/validation-error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle HttpMessageNotReadableException with malformed JSON")
    void shouldHandleHttpMessageNotReadableException() throws Exception {
        mockMvc.perform(post("/test/malformed-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request. Please verify the data format, such as correct date/time patterns (e.g., 'HH:mm'), exact Enum values, and proper JSON syntax."))
                .andExpect(jsonPath("$.path").value("/test/malformed-json"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle MissingServletRequestParameterException")
    void shouldHandleMissingServletRequestParameterException() throws Exception {
        mockMvc.perform(get("/test/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Missing required parameter: requiredParam"))
                .andExpect(jsonPath("$.path").value("/test/missing-param"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle MethodArgumentTypeMismatchException")
    void shouldHandleMethodArgumentTypeMismatchException() throws Exception {
        mockMvc.perform(get("/test/type-mismatch/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid format for parameter: id"))
                .andExpect(jsonPath("$.path").value("/test/type-mismatch/not-a-number"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle EntityNotFoundException with 404 Not Found")
    void shouldHandleEntityNotFoundException() throws Exception {
        mockMvc.perform(get("/test/entity-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("The requested resource was not found."))
                .andExpect(jsonPath("$.path").value("/test/entity-not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle generic Exception with 500 Internal Server Error")
    void shouldHandleGenericException() throws Exception {
        mockMvc.perform(post("/test/generic-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected server error occurred."))
                .andExpect(jsonPath("$.path").value("/test/generic-exception"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
