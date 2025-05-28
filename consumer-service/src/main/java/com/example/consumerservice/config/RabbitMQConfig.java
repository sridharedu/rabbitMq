package com.example.consumerservice.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "user.direct";

    @Bean
    public DirectExchange userDirectExchange() {
        // This service also declares the 'user.direct' exchange.
        // Declaring the exchange in both producer and consumer ensures that it exists
        // before either service tries to use it. It's an idempotent operation.
        // The consumer needs this to bind its queues to the exchange.
        return new DirectExchange(EXCHANGE_NAME);
    }
}
