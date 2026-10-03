package sodresoftwares.barbearia.services.billing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.billing.*;
import sodresoftwares.barbearia.repositories.billing.PaymentRepository;
import sodresoftwares.barbearia.repositories.billing.SubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static java.time.Instant.now;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentWebhookService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionHistoryService subscriptionHistoryService;

    @Transactional
    public void processWebhook(Map<String, Object> payload) {
        String event = (String) payload.get("event");

        @SuppressWarnings("unchecked")
        Map<String, Object> paymentData = (Map<String, Object>) payload.get("payment");

        if (event == null || paymentData == null) {
            return;
        }

        String gatewayInvoiceId = (String) paymentData.get("id");
        String businessId = (String) paymentData.get("externalReference");

        log.info("Webhook received: {} for invoice {}", event, gatewayInvoiceId);

        switch (event) {
            case "PAYMENT_CREATED" -> handlePaymentCreated(paymentData, businessId);
            case "PAYMENT_RECEIVED", "PAYMENT_CONFIRMED" -> handlePaymentPaid(gatewayInvoiceId);
            case "PAYMENT_OVERDUE" -> handlePaymentOverdue(gatewayInvoiceId);
            case "PAYMENT_REFUNDED" -> handlePaymentRefunded(gatewayInvoiceId);
            default -> log.debug("Unhandled event type: {}", event);
        }
    }

    private void handlePaymentCreated(Map<String, Object> paymentData, String businessId) {
        String gatewayInvoiceId = (String) paymentData.get("id");

        // Idempotency check: prevents creating duplicate payments if Asaas resends the webhook
        if (paymentRepository.existsByGatewayInvoiceId(gatewayInvoiceId)) {
            log.debug("Payment {} already exists. Ignoring.", gatewayInvoiceId);
            return;
        }

        Subscription subscription = subscriptionRepository.findByBusinessId(businessId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "SUBSCRIPTION_NOT_FOUND",
                        "Subscription not found for webhook processing."
                ));

        Payment payment = Payment.builder()
                .subscription(subscription)
                .gatewayInvoiceId(gatewayInvoiceId)
                .paymentProvider(PaymentProvider.ASAAS)
                .amount(new BigDecimal(paymentData.get("value").toString()))
                .dueDate(LocalDate.parse((String) paymentData.get("dueDate")))
                .status(PaymentStatus.PENDING)
                .invoiceUrl((String) paymentData.get("invoiceUrl"))
                .build();

        paymentRepository.save(payment);
        log.info("Payment PENDING created for invoice {}", gatewayInvoiceId);
    }

    private void handlePaymentPaid(String gatewayInvoiceId) {
        Payment payment = paymentRepository.findByGatewayInvoiceId(gatewayInvoiceId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "PAYMENT_NOT_FOUND",
                        "Payment not found for confirmation."
                ));

        // Idempotency check: prevents extending the subscription twice
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.debug("Payment {} is already PAID. Ignoring.", gatewayInvoiceId);
            return;
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(now());
        paymentRepository.save(payment);

        extendSubscription(payment);
    }

    private void handlePaymentOverdue(String gatewayInvoiceId) {
        Payment payment = paymentRepository.findByGatewayInvoiceId(gatewayInvoiceId).orElse(null);

        if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
            Subscription subscription = payment.getSubscription();

            if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                payment.setStatus(PaymentStatus.OVERDUE);
                log.info("Payment {} marked as OVERDUE (Real debt).", gatewayInvoiceId);
            } else {
                payment.setStatus(PaymentStatus.EXPIRED);
                log.info("Payment {} marked as EXPIRED (Abandoned checkout).", gatewayInvoiceId);
            }

            paymentRepository.save(payment);
        }
    }
    private void handlePaymentRefunded(String gatewayInvoiceId) {
        Payment payment = paymentRepository.findByGatewayInvoiceId(gatewayInvoiceId).orElse(null);

        if (payment != null) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            Subscription subscription = payment.getSubscription();

            subscriptionHistoryService.logStatusChange(
                    subscription,
                    SubscriptionStatus.SUSPENDED,
                    "WEBHOOK: PAYMENT_REFUNDED"
            );

            subscription.setStatus(SubscriptionStatus.SUSPENDED);
            subscriptionRepository.save(subscription);

            log.info("Payment {} REFUNDED. Subscription {} suspended immediately.",
                    gatewayInvoiceId,
                    subscription.getId()
            );
        }
    }

    private void extendSubscription(Payment payment) {
        Subscription lockedSubscription = subscriptionRepository.findByIdWithLock(payment.getSubscription().getId())
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "SUBSCRIPTION_NOT_FOUND",
                        "Subscription not found for extension."
                ));

        Plan plan = lockedSubscription.getPlan();

        LocalDate newPeriodEnd;

        if (plan.getBillingCycle() == BillingCycle.YEARLY) {
            double fractionPaid = payment.getAmount().doubleValue() / plan.getPrice().doubleValue();

            long daysToAdd = Math.round(fractionPaid * 365);
            newPeriodEnd = lockedSubscription.getCurrentPeriodEnd().plusDays(daysToAdd);

            log.info("Annual plan installment: {}% paid. Adding {} days.",
                    String.format("%.2f", fractionPaid * 100), daysToAdd);
        } else {
            newPeriodEnd = lockedSubscription.getCurrentPeriodEnd().plusMonths(1);
        }

        subscriptionHistoryService.logStatusChange(
                lockedSubscription,
                SubscriptionStatus.ACTIVE,
                "WEBHOOK: PAYMENT_CONFIRMED"
        );

        lockedSubscription.setCurrentPeriodEnd(newPeriodEnd);
        lockedSubscription.setStatus(SubscriptionStatus.ACTIVE);

        subscriptionRepository.save(lockedSubscription);
        log.info("Subscription {} extended. New end date: {}", lockedSubscription.getId(), newPeriodEnd);
    }
}