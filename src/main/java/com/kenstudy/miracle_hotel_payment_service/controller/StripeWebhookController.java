package com.kenstudy.miracle_hotel_payment_service.controller;

import com.google.gson.JsonSyntaxException;
import com.kenstudy.miracle_hotel_payment_service.constant.PaymentConstant;
import com.kenstudy.miracle_hotel_payment_service.model.Payment;
import com.kenstudy.miracle_hotel_payment_service.repository.PaymentRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.ApiResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping
public class StripeWebhookController {
    private PaymentRepository paymentRepository;

    public StripeWebhookController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Value("${stripe.webhook.secret.key}")
    String endpointSecret;

    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
                                                      @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            event = ApiResource.GSON.fromJson(payload, Event.class);
        } catch (JsonSyntaxException e) {
            log.error("Invalid payload: {}", payload, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded(event);
                break;
            case "checkout.session.expired":
            case "checkout.session.completed":
            case "charge.succeeded":
            case "charge.updated":
            case "payment_intent.created":
                // Silently ignore these events
                break;
            default:
                log.warn("Unhandled event type: {}", event.getType());
                break;
        }

        return ResponseEntity.ok("Webhook received");
    }

    private void handlePaymentIntentSucceeded(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            log.error("PaymentIntent object not found in webhook event. Raw event data: {}", event);
            return;  // Early exit if the object is not present
        }
        if (!(stripeObject instanceof PaymentIntent paymentIntent)) {
            log.error("Deserialized object is not a PaymentIntent. Raw event data: {}", event);
            return;
        }

        String paymentStatus = paymentIntent.getStatus();
        String paymentIntentId = paymentIntent.getId();
        log.info("Payment Status: {}", paymentStatus);
        log.info("Payment Intent ID: {}", paymentIntentId);
        String customerId = paymentIntent.getCustomer();

        if (customerId != null) {
            List<Payment> paymentsByCustomerId = paymentRepository.findPaymentsByCustomerId(customerId);

            if (!paymentsByCustomerId.isEmpty()) {
                paymentsByCustomerId.forEach(payment -> {
                    if ("succeeded".equals(paymentStatus)) {
                        payment.setStatus(PaymentConstant.PAYMENT_COMPLETED.name());
                        payment.setDate(new Date());
                        try {
                            paymentRepository.save(payment);
                            log.info("Payment status updated to SUCCESS for Payment Intent ID: {}", paymentIntentId);
                        } catch (Exception e) {
                            log.error("Error saving payment entity for Payment Intent ID: {}", paymentIntentId, e);
                        }
                    }
                });
            } else {
                log.warn("No Payment record found for Customer ID: {}", customerId);
            }
        } else {
            log.warn("Payment Intent has no associated Customer ID");
        }
    }

}

