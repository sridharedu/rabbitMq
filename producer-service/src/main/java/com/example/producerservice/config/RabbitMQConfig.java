package com.example.producerservice.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "user.direct";

    @Bean
    public DirectExchange userDirectExchange() {
        // Direct exchange routes messages to queues whose binding key exactly matches the message's routing key.
        // This is useful for 1:1 message delivery to a specific consumer (e.g., a specific service instance or task handler).
        // For instance, a 'user_signup' routing key would only go to queues bound with 'user_signup'.
        return new DirectExchange(EXCHANGE_NAME);
    }
}
