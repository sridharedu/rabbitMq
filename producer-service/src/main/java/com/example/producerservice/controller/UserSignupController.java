package com.example.producerservice.controller;

import com.example.producerservice.config.RabbitMQConfig;
import com.example.producerservice.event.UserSignupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1") // Base path for the API
public class UserSignupController {

    private static final Logger logger = LoggerFactory.getLogger(UserSignupController.class);

    private final RabbitTemplate rabbitTemplate;
    // No need to inject DirectExchange bean itself if we use its name directly with routing key.
    // RabbitTemplate is smart enough to find it if configured correctly.

    @Autowired
    public UserSignupController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> handleUserSignup(@RequestBody UserSignupEvent event) {
        if (event.getUserId() == null || event.getEmail() == null) {
            // Basic validation
            return ResponseEntity.badRequest().body("User ID and Email are required.");
        }
        
        // The producer sends a message to an exchange, not directly to a queue.
        // This decouples the producer from the consumer(s). The producer doesn't need to know
        // which queue(s) will receive the message, or how many consumers there are.
        // The exchange is responsible for routing the message based on its type and binding rules.
        // Here, we use the direct exchange and the specific routing key for user signups.
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.DIRECT_EXCHANGE_NAME, RabbitMQConfig.USER_SIGNUP_ROUTING_KEY, event);
            logger.info("Sent UserSignupEvent to exchange '{}' with routing key '{}': {}", RabbitMQConfig.DIRECT_EXCHANGE_NAME, RabbitMQConfig.USER_SIGNUP_ROUTING_KEY, event);
            return ResponseEntity.ok("Signup event sent successfully for user: " + event.getUserId());
        } catch (Exception e) {
            logger.error("Error sending UserSignupEvent for user {}: {}", event.getUserId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to send signup event due to an internal error.");
        }
    }
}
