package com.example.flashsale.consumer.config;

import com.example.flashsale.sharedkernel.config.RabbitMQConfigConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ErrorHandler;


@Configuration
public class RabbitMQConsumerConfig {

    // Allow shard_id to be configured via environment or application.properties
    // Default to shard 0 for now if not specified.
    @Value("${flashsale.consumer.shard-id:0}")
    private int consumerShardId;

    // Make NUM_USER_SHARDS configurable or ensure it matches producer
    // For now, using the constant from shared-kernel for queue/routing key generation
    // but the actual number of consumer instances will determine parallelism.

    @Bean
    public String consumedQueueName() {
        return RabbitMQConfigConstants.getUserShardQueueName(consumerShardId);
    }

    @Bean
    public String consumedRoutingKey() {
        return RabbitMQConfigConstants.getOrderRoutingKey(consumerShardId);
    }

    @Bean
    public Exchange regionalOrdersExchange() {
        // Should match the exchange declared by the producer
        return ExchangeBuilder.topicExchange(RabbitMQConfigConstants.REGIONAL_ORDERS_EXCHANGE)
                              .durable(true)
                              .build();
    }

    @Bean
    public Queue userShardQueue(String consumedQueueName, AmqpAdmin amqpAdmin) {
        // DLX configuration
        String dlxName = RabbitMQConfigConstants.REGIONAL_ORDERS_DLX;
        String dlqName = RabbitMQConfigConstants.REGIONAL_ORDERS_DEAD_LETTER_QUEUE_NAME;
        String commonDlqRoutingKey = RabbitMQConfigConstants.COMMON_DLQ_ROUTING_KEY;

        // Declare DLX (idempotent)
        Exchange dlx = ExchangeBuilder.directExchange(dlxName).durable(true).build();
        amqpAdmin.declareExchange(dlx);

        // Declare DLQ (idempotent)
        Queue deadLetterQueue = QueueBuilder.durable(dlqName).build();
        amqpAdmin.declareQueue(deadLetterQueue);

        // Bind DLQ to DLX (idempotent)
        Binding dlqBinding = BindingBuilder.bind(deadLetterQueue).to(dlx).with(commonDlqRoutingKey).noargs();
        amqpAdmin.declareBinding(dlqBinding);

        return QueueBuilder.durable(consumedQueueName)
                        .withArgument("x-dead-letter-exchange", dlxName)
                        .withArgument("x-dead-letter-routing-key", commonDlqRoutingKey)
                        .build();
    }

    @Bean
    public Binding userShardQueueBinding(Queue userShardQueue, Exchange regionalOrdersExchange, String consumedRoutingKey) {
        return BindingBuilder.bind(userShardQueue)
                                     .to(regionalOrdersExchange)
                                     .with(consumedRoutingKey)
                                     .noargs();
    }

    @Bean
    public Jackson2JsonMessageConverter consumerJackson2MessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Important for Instant type
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter consumerJackson2MessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(consumerJackson2MessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // Crucial for manual ack
        factory.setPrefetchCount(1); // Process one message at a time per listener for strict ordering

        // Set a default error handler that NACKs and requeues=false for unhandled exceptions
        // This ensures messages go to DLQ if not handled by listener's try-catch
        factory.setErrorHandler(new ConditionalRejectingErrorHandler(new FatalExceptionStrategy()));

        // To scale, you can increase concurrency, but for strict FIFO per queue, concurrency should be 1.
        // Scaling is achieved by having multiple consumer instances, each listening to a different shard queue.
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        return factory;
    }

    // Custom fatal exception strategy for the error handler
    // This ensures that any exception not caught by the listener itself will be treated as fatal,
    // causing the message to be rejected and sent to the DLQ (if configured).
    public static class FatalExceptionStrategy extends ConditionalRejectingErrorHandler.DefaultExceptionStrategy {
        @Override
        public boolean isFatal(Throwable t) {
            // Example: Treat ListenerExecutionFailedException containing specific business exceptions as non-fatal for retry
            if (t instanceof ListenerExecutionFailedException) {
                 // Throwable cause = ((ListenerExecutionFailedException) t).getFailedMessage().getPayload();
                 // if (cause instanceof MyRetryableBusinessException) return false;
            }
            // By default, all exceptions are fatal for this strategy unless overridden
            System.err.println("ErrorHandler: Fatal exception encountered, message will be rejected: " + t.getMessage());
            return true;
        }
    }
}
