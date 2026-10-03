package sodresoftwares.barbearia.controllers.billing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sodresoftwares.barbearia.infra.security.SecurityFilter;
import sodresoftwares.barbearia.infra.security.SubscriptionCheckInterceptor;
import sodresoftwares.barbearia.infra.security.WebMvcConfig;
import sodresoftwares.barbearia.services.billing.PaymentWebhookService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PaymentWebhookController.class,
        properties = "asaas.webhook.token=valid-test-token",
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
@DisplayName("PaymentWebhookController Tests")
class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentWebhookService webhookService;

    @MockitoBean
    private CacheManager cacheManager;

    private final String VALID_TOKEN = "valid-test-token";
    private final String INVALID_TOKEN = "invalid-token";

    @Test
    @DisplayName("Should process webhook and return 200 OK when token is valid")
    void shouldHandleWebhookWithValidToken() throws Exception {
        String payload = "{\"event\": \"PAYMENT_CONFIRMED\"}";

        mockMvc.perform(post("/webhooks/asaas")
                        .header("asaas-access-token", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(webhookService).processWebhook(any(java.util.Map.class));
    }

    @Test
    @DisplayName("Should return 401 UNAUTHORIZED when token is invalid")
    void shouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {
        String payload = "{\"event\": \"PAYMENT_CONFIRMED\"}";

        mockMvc.perform(post("/webhooks/asaas")
                        .header("asaas-access-token", INVALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        verify(webhookService, never()).processWebhook(any());
    }

    @Test
    @DisplayName("Should return 401 UNAUTHORIZED when token is missing")
    void shouldReturnUnauthorizedWhenTokenIsMissing() throws Exception {
        String payload = "{\"event\": \"PAYMENT_CONFIRMED\"}";

        mockMvc.perform(post("/webhooks/asaas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        verify(webhookService, never()).processWebhook(any());
    }
}