package sodresoftwares.barbearia.controllers.billing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sodresoftwares.barbearia.dto.billing.CheckoutRequest;
import sodresoftwares.barbearia.dto.billing.SubscriptionResponseDTO;
import sodresoftwares.barbearia.infra.security.SecurityFilter;
import sodresoftwares.barbearia.infra.security.SubscriptionCheckInterceptor;
import sodresoftwares.barbearia.infra.security.WebMvcConfig;
import sodresoftwares.barbearia.model.billing.SubscriptionStatus;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.services.billing.SubscriptionService;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SubscriptionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        SecurityFilter.class,
                        SubscriptionCheckInterceptor.class,
                        WebMvcConfig.class
                }
        ),
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
@DisplayName("SubscriptionController Tests")
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JacksonTester<Object> jsonTester;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private CacheManager cacheManager;

    private User loggedUser;

    @BeforeEach
    void setUp() {
        loggedUser = User.builder().id("user-123").login("test@test.com").build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loggedUser, null, null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should generate checkout URL successfully")
    void shouldGenerateCheckoutUrl() throws Exception {
        CheckoutRequest request = new CheckoutRequest("biz-123", "PRO_MONTHLY");
        when(subscriptionService.generateCheckout("biz-123", "PRO_MONTHLY")).thenReturn("https://checkout.url");

        mockMvc.perform(post("/subscriptions/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTester.write(request).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.url"));

        verify(subscriptionService).generateCheckout("biz-123", "PRO_MONTHLY");
    }

    @Test
    @DisplayName("Should cancel subscription successfully")
    void shouldCancelSubscription() throws Exception {
        mockMvc.perform(post("/subscriptions/cancel/biz-123"))
                .andExpect(status().isNoContent());

        verify(subscriptionService).cancelSubscription("biz-123");
    }

    @Test
    @DisplayName("Should get logged user subscription details")
    void shouldGetMySubscription() throws Exception {
        SubscriptionResponseDTO responseDTO = new SubscriptionResponseDTO(
                SubscriptionStatus.ACTIVE,
                "PRO_MONTHLY",
                LocalDate.now().plusDays(30),
                null
        );

        when(subscriptionService.getMySubscriptionDetails(nullable(String.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(get("/subscriptions/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.planName").value("PRO_MONTHLY"));

        verify(subscriptionService).getMySubscriptionDetails(nullable(String.class));
    }
}