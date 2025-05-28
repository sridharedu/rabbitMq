package com.example.producerservice.controller;

import com.example.producerservice.config.RabbitMQConfig;
import com.example.producerservice.event.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1") // Base path for the API
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public PaymentController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/payment")
    public ResponseEntity<String> handlePayment(@RequestBody PaymentEvent event) {
        if (event.getPaymentId() == null || event.getAmount() == null || event.getCurrency() == null || event.getPaymentType() == null) {
            return ResponseEntity.badRequest().body("Payment ID, Amount, Currency, and Payment Type are required.");
        }

        // Constructing a dynamic routing key based on the payment type.
        // For example, if paymentType is "card", routing key becomes "payment.card".
        // If paymentType is "wallet", routing key becomes "payment.wallet".
        // This allows consumers to subscribe to specific types of payment events
        // using wildcard patterns like 'payment.card.*' or 'payment.wallet.#' or just 'payment.*'.
        String routingKey = "payment." + event.getPaymentType().toLowerCase(Locale.ROOT).replaceAll("\s+", "_");

        try {
            // Messages are sent to the 'events.topic' exchange.
            // The topic exchange then routes the message to bound queues based on the routing key pattern match.
            // This provides great flexibility for consumers to subscribe to events they are interested in.
            rabbitTemplate.convertAndSend(RabbitMQConfig.TOPIC_EXCHANGE_NAME, routingKey, event);
            logger.info("Sent PaymentEvent to exchange '{}' with routing key '{}': {}", RabbitMQConfig.TOPIC_EXCHANGE_NAME, routingKey, event);
            return ResponseEntity.ok("Payment event sent successfully for ID: " + event.getPaymentId() + " with type: " + event.getPaymentType());
        } catch (Exception e) {
            logger.error("Error sending PaymentEvent for ID {}: {}", event.getPaymentId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to send payment event due to an internal error.");
        }
    }
}
