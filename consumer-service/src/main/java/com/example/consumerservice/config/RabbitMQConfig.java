package com.example.consumerservice.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange; // Added import
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DIRECT_EXCHANGE_NAME = "user.direct"; // Renamed for clarity
    public static final String TOPIC_EXCHANGE_NAME = "events.topic"; // New constant

    @Bean
    public DirectExchange userDirectExchange() {
        // This service also declares the 'user.direct' exchange.
        // Declaring the exchange in both producer and consumer ensures that it exists
        // before either service tries to use it. It's an idempotent operation.
        // The consumer needs this to bind its queues to the exchange.
        return new DirectExchange(DIRECT_EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange eventsTopicExchange() {
        // This service also declares the 'events.topic' exchange.
        // As with the direct exchange, this ensures it exists for queue binding.
        // Consumers use topic exchanges to subscribe to messages based on patterns.
        // For example, a consumer might bind a queue with 'payment.*' to receive all payment-related events,
        // or 'user.created' to specifically get user creation events.
        // Another service might subscribe to '*.updated' to get all update events.
        return new TopicExchange(TOPIC_EXCHANGE_NAME);
    }
}
