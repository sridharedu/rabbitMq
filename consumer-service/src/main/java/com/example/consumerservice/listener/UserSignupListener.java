package com.example.consumerservice.listener;

import com.example.consumerservice.config.RabbitMQConfig;
import com.example.consumerservice.event.UserSignupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UserSignupListener {

    private static final Logger logger = LoggerFactory.getLogger(UserSignupListener.class);

    /**
     * Listens to messages on the USER_SIGNUP_QUEUE_NAME.
     * Spring AMQP, with Jackson auto-configured, will attempt to deserialize the JSON message
     * payload into a UserSignupEvent object.
     *
     * Acknowledgment Mode (ack-mode):
     * By default, Spring AMQP uses AUTO acknowledgment mode if not specified otherwise
     * globally (e.g., in application.yml via spring.rabbitmq.listener.simple.default-acknowledge-mode)
     * or per listener.
     *
     * AUTO Acknowledgment:
     * - The message is considered acknowledged by the RabbitMQ broker as soon as it's successfully
     *   delivered to this listener method (i.e., no exception is thrown upon invocation).
     * - If this method executes without throwing an exception, the message is removed from the queue.
     * - Pros: Simple to use, no manual ack code needed.
     * - Cons: If the application crashes *during* the processing of this method (after delivery but
     *   before completion of business logic), the message is lost because it was already ack'd.
     * - Suitable for:
     *     - Idempotent operations where reprocessing a lost message (if it were re-queued) would be safe.
     *     - Situations where a small chance of message loss upon consumer failure is acceptable.
     *     - Very fast, non-critical processing.
     *
     * For critical messages where "at-least-once" processing is required even in failure scenarios,
     * MANUAL acknowledgment mode should be used (covered in RABBIT-012).
     */
    @RabbitListener(queues = RabbitMQConfig.USER_SIGNUP_QUEUE_NAME)
    public void handleUserSignup(UserSignupEvent event) {
        logger.info("Received UserSignupEvent (auto-ack) from queue '{}': {}", RabbitMQConfig.USER_SIGNUP_QUEUE_NAME, event);

        // Business logic for processing the user signup event would go here.
        // For RABBIT-011, we are just logging. Persistence will be added in RABBIT-017.
        // Example: userService.registerUser(event);

        logger.info("Successfully processed UserSignupEvent for userId: {}", event.getUserId());
    }
}
