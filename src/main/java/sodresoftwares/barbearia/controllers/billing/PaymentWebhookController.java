package sodresoftwares.barbearia.controllers.billing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.barbearia.services.billing.PaymentWebhookService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhooks/asaas")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService webhookService;

    @Value("${asaas.webhook.token}")
    private String asaasWebhookToken;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "asaas-access-token", required = false) String token,
            @RequestBody Map<String, Object> payload) {
        if (token == null || !token.equals(asaasWebhookToken)) {
            log.warn("Fraud attempt or invalid webhook token. Received token: {}", token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        webhookService.processWebhook(payload);
        return ResponseEntity.ok().build();
    }
}