package com.example.flashsale.ingestion.controller;

import com.example.flashsale.sharedkernel.config.RabbitMQConfigConstants;
import com.example.flashsale.sharedkernel.dto.Order;
import jakarta.validation.Valid;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public OrderController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping
    public ResponseEntity<String> submitOrder(@Valid @RequestBody Order orderRequest) {
        // Enrich order with server-side details if needed
        if (orderRequest.getOrderId() == null || orderRequest.getOrderId().isBlank()) {
            orderRequest.setOrderId(UUID.randomUUID().toString());
        }
        if (orderRequest.getTimestamp() == null) {
            orderRequest.setTimestamp(Instant.now());
        }

        String routingKey = RabbitMQConfigConstants.getOrderRoutingKey(
            Math.abs(orderRequest.getUserId().hashCode()) % RabbitMQConfigConstants.NUM_USER_SHARDS
        );

        try {
            // CorrelationData is useful for matching publisher confirms
            CorrelationData correlationData = new CorrelationData(orderRequest.getOrderId());

            System.out.println("Publishing order: " + orderRequest.getOrderId() + " with routing key: " + routingKey);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfigConstants.REGIONAL_ORDERS_EXCHANGE,
                    routingKey,
                    orderRequest,
                    message -> {
                        // Can set message properties here if needed, like headers
                        // For example, message.getMessageProperties().setCorrelationId(orderRequest.getOrderId());
                        return message;
                    },
                    correlationData
            );

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                                 .body("Order received and sent for processing: " + orderRequest.getOrderId());
        } catch (Exception e) {
            System.err.println("Error publishing order to RabbitMQ: " + e.getMessage());
            // Log exception e
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Failed to send order for processing: " + e.getMessage());
        }
    }
}
