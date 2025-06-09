package com.example.flashsale.ingestion.config;

import com.example.flashsale.sharedkernel.config.RabbitMQConfigConstants;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQProducerConfig {

    @Bean
    public Exchange regionalOrdersExchange() {
        return ExchangeBuilder.topicExchange(RabbitMQConfigConstants.REGIONAL_ORDERS_EXCHANGE)
                              .durable(true)
                              .build();
    }

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        // For sending Order DTO as JSON
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory,
                                         final Jackson2JsonMessageConverter producerJackson2MessageConverter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(producerJackson2MessageConverter);

        // Enable publisher confirms
        // Note: For this to work, the underlying ConnectionFactory must also support publisher confirms.
        // Spring Boot auto-configures this if spring.rabbitmq.publisher-confirm-type=correlated (or simple) is set.
        rabbitTemplate.setMandatory(true); // Ensures messages are returned if not routable (requires a ReturnCallback)

        // Optional: Add a ConfirmCallback and ReturnCallback for logging/handling
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData != null) {
                System.out.println("Publisher confirm received for correlation id: " + correlationData.getId() + ", ack: " + ack + ", cause: " + cause);
            } else {
                System.out.println("Publisher confirm received (no correlation), ack: " + ack + ", cause: " + cause);
            }
            if (!ack) {
                System.err.println("Message FAILED to send to exchange. Cause: " + cause);
                // Handle failed publish (e.g., log, alert, retry with different broker)
            }
        });

        rabbitTemplate.setReturnsCallback(returned -> {
            System.err.println("Message returned: " + returned.getMessage());
            System.err.println("Reply Code: " + returned.getReplyCode());
            System.err.println("Reply Text: " + returned.getReplyText());
            System.err.println("Exchange: " + returned.getExchange());
            System.err.println("Routing Key: " + returned.getRoutingKey());
            // Handle unroutable message (e.g., log, alert)
        });

        return rabbitTemplate;
    }
}
