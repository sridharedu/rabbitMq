package com.example.flashsale.consumer.listener;

import com.example.flashsale.consumer.service.IdempotencyStore;
import com.example.flashsale.consumer.service.StockService; // Import StockService
import com.example.flashsale.sharedkernel.dto.Order;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OrderListener {

    private static final Logger log = LoggerFactory.getLogger(OrderListener.class);
    private final IdempotencyStore idempotencyStore;
    private final StockService stockService; // Add StockService field

    @Autowired
    public OrderListener(IdempotencyStore idempotencyStore, StockService stockService) { // Update constructor
        this.idempotencyStore = idempotencyStore;
        this.stockService = stockService; // Initialize StockService
    }

    @RabbitListener(queues = "#{consumedQueueName}", containerFactory = "rabbitListenerContainerFactory")
    public void handleOrderMessage(@Payload Order order,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        String queueName = "#{@consumedQueueName}"; // Simplified queue name for logging

        log.info("Received order on queue '{}': Order ID: {}, User ID: {}",
                 queueName, order.getOrderId(), order.getUserId());

        IdempotencyStore.ProcessingState initialState = idempotencyStore.checkAndSetProcessing(order.getOrderId());

        switch (initialState) {
            case ALREADY_PROCESSED:
                log.info("Order {} already processed. Acknowledging.", order.getOrderId());
                channel.basicAck(delivery_tag, false);
                return;
            case CURRENTLY_PROCESSING:
                log.warn("Order {} is currently being processed elsewhere. Nacking with requeue.", order.getOrderId());
                channel.basicNack(delivery_tag, false, true);
                return;
            case OK_TO_PROCESS:
                log.info("Order {} is OK to process.", order.getOrderId());
                break;
        }

        try {
            // STEP 2: Stock Check
            if (!stockService.decreaseStock(order.getItemId(), order.getQuantity())) {
               log.warn("Stock unavailable or item not found for order {}. Item: {}, Qty: {}. Nacking (to DLQ).",
                        order.getOrderId(), order.getItemId(), order.getQuantity());
               idempotencyStore.clearProcessingMark(order.getOrderId());
               channel.basicNack(delivery_tag, false, false); // To DLQ
               return;
            }
            log.info("Stock confirmed and decreased for order {}", order.getOrderId());

            log.info("Simulating main processing for order: {}", order.getOrderId());
            Thread.sleep(100);

            idempotencyStore.markAsProcessed(order.getOrderId());
            log.info("Successfully processed order: {}. Acknowledging.", order.getOrderId());
            channel.basicAck(delivery_tag, false);

        } catch (InterruptedException e) {
            log.warn("Processing interrupted for order: {}. Nacking with requeue.", order.getOrderId());
            // Attempt to rollback stock if business logic requires (complex, not implemented here)
            // For now, idempotency mark is cleared, and message is requeued.
            // If stock was already decremented, a retry might try to decrement again.
            // This highlights the need for idempotent stock operations or compensation logic.
            idempotencyStore.clearProcessingMark(order.getOrderId());
            Thread.currentThread().interrupt();
            channel.basicNack(delivery_tag, false, true);
        } catch (Exception e) {
            log.error("Unhandled exception processing order: {}. Error: {}. Nacking (to DLQ).",
                      order.getOrderId(), e.getMessage(), e);
            // Attempt to rollback stock (complex, not implemented here)
            idempotencyStore.clearProcessingMark(order.getOrderId());
            channel.basicNack(delivery_tag, false, false);
        }
    }
}
