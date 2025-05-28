package com.example.consumerservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges
    public static final String DIRECT_EXCHANGE_NAME = "user.direct";
    public static final String TOPIC_EXCHANGE_NAME = "events.topic";

    // Queues & Routing Keys for Direct Exchange
    public static final String USER_SIGNUP_QUEUE_NAME = "q.user.signup";
    public static final String USER_SIGNUP_ROUTING_KEY = "user_signup_key";

    // Queues & Routing Patterns for Topic Exchange
    public static final String PAYMENT_EVENTS_QUEUE_NAME = "q.events.payment";
    public static final String PAYMENT_EVENTS_ROUTING_PATTERN = "payment.*"; // Capture all payment-related events

    public static final String ALL_EVENTS_QUEUE_NAME = "q.events.all";
    public static final String ALL_EVENTS_ROUTING_PATTERN = "#"; // Capture all events from the topic exchange

    // Exchange Beans
    @Bean
    public DirectExchange userDirectExchange() {
        return new DirectExchange(DIRECT_EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange eventsTopicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE_NAME);
    }

    // Queue & Binding for User Signup (Direct Exchange)
    @Bean
    public Queue userSignupQueue() {
        // Durable queues survive broker restarts.
        return new Queue(USER_SIGNUP_QUEUE_NAME, true); // true for durable
    }

    @Bean
    public Binding userSignupBinding(Queue userSignupQueue, DirectExchange userDirectExchange) {
        // Binds userSignupQueue to userDirectExchange with specific routing key.
        return BindingBuilder.bind(userSignupQueue).to(userDirectExchange).with(USER_SIGNUP_ROUTING_KEY);
    }

    // Queues & Bindings for Event Processing (Topic Exchange)
    @Bean
    public Queue paymentEventsQueue() {
        // This queue will receive messages published to 'events.topic' with a routing key starting with 'payment.'
        // e.g., 'payment.card', 'payment.wallet.success', etc.
        // It's durable, so messages persist across broker restarts.
        return new Queue(PAYMENT_EVENTS_QUEUE_NAME, true);
    }

    @Bean
    public Binding paymentEventsBinding(Queue paymentEventsQueue, TopicExchange eventsTopicExchange) {
        // Binds paymentEventsQueue to eventsTopicExchange with the pattern 'payment.*'.
        // '*' matches exactly one word. So, 'payment.card' would match, but 'payment.card.details' would not
        // if this consumer is only interested in the first level after 'payment'.
        // If deeper matching is needed, 'payment.#' could be used.
        return BindingBuilder.bind(paymentEventsQueue).to(eventsTopicExchange).with(PAYMENT_EVENTS_ROUTING_PATTERN);
    }

    @Bean
    public Queue allEventsQueue() {
        // This queue is a catch-all for any message published to 'events.topic'.
        // Useful for logging, auditing, or debugging all events flowing through the system.
        // Also durable.
        return new Queue(ALL_EVENTS_QUEUE_NAME, true);
    }

    @Bean
    public Binding allEventsBinding(Queue allEventsQueue, TopicExchange eventsTopicExchange) {
        // Binds allEventsQueue to eventsTopicExchange with the pattern '#'.
        // '#' matches zero or more words, separated by dots. This means any routing key will match.
        // For example, 'user.created', 'payment.card.failed', 'inventory.item.added' would all be routed here.
        return BindingBuilder.bind(allEventsQueue).to(eventsTopicExchange).with(ALL_EVENTS_ROUTING_PATTERN);
    }
}
