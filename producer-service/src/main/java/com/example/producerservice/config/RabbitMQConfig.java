package com.example.producerservice.config;

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
        // Direct exchange routes messages to queues whose binding key exactly matches the message's routing key.
        // This is useful for 1:1 message delivery to a specific consumer (e.g., a specific service instance or task handler).
        // For instance, a 'user_signup' routing key would only go to queues bound with 'user_signup'.
        return new DirectExchange(DIRECT_EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange eventsTopicExchange() {
        // Topic exchange routes messages to queues based on wildcard matches between the routing key and the routing pattern
        // specified when binding the queue to the exchange.
        // '*' matches a single word, '#' matches zero or more words.
        // Use cases:
        // - Categorized event streams (e.g., routing keys like 'payment.card.approved', 'payment.wallet.failed').
        // - Flexible subscriptions (e.g., a queue bound with 'payment.*.approved' gets all approved payments,
        //   another bound with '#.failed' gets all failed events from any source).
        return new TopicExchange(TOPIC_EXCHANGE_NAME);
    }
}
