# Real-Time Order Processing System Architecture

This document outlines the architecture for the Real-Time Order Processing System, designed to handle a global flash sale.

## Components:

1.  **Order Ingestion Service:**
    *   Receives orders from users (e.g., via REST API).
    *   Performs initial validation (schema, basic business rules).
    *   Assigns a unique `orderId` and captures `userId`.
    *   Publishes orders to a regional RabbitMQ exchange using publisher confirms for reliability.
    *   Routing key could be based on `userId` (e.g., `orders.user.<userId_hash_shard>`) or a general topic like `orders.new`.

2.  **Regional RabbitMQ Setup (Per Region):**
    *   **Exchange:** `regional_orders_exchange` (Topic Exchange).
    *   **Queues:** A set of "user shard queues" (e.g., `user_shard_queue_1`, `user_shard_queue_N`). Orders are routed here based on a consistent hash of `userId` to ensure messages for the same user land in the same queue for sequential processing. These queues are durable.
    *   **Bindings:** Bind `user_shard_queue_X` to `regional_orders_exchange` with routing keys like `orders.user.shardX`.
    *   **Dead Letter Exchange (DLX):** `regional_dlx`. For orders failing initial regional processing.
    *   **Regional Retry Queues:** For transient issues at the regional level, with delayed redelivery.

3.  **Regional Order Consumers:**
    *   Consume from their respective regional `user_shard_queue`. Manual acknowledgements are crucial.
    *   **Responsibilities:**
        *   **Deduplication (Idempotency):** Check `orderId` against a distributed store (e.g., Redis, Cassandra) to see if it's already processed or in progress. Mark as in-progress.
        *   **(Bonus) Order Enrichment:** e.g., Currency conversion by calling an external service (could involve another request-response via queues).
        *   **(Bonus) Order Cancellation Check:** If a cancellation request for this `orderId` is found (see Component 10), mark order as cancelled and ack.
        *   Forward valid, non-duplicate, non-cancelled orders to the Global RabbitMQ Exchange. Add regional context if needed.
    *   Use prefetch count for flow control.

4.  **Global RabbitMQ Setup (Central):**
    *   **Exchange:** `global_orders_exchange` (Topic Exchange). Orders arrive here from regional consumers.
    *   **Queues (Durable):**
        *   `payment_processing_queue`: For orders needing payment.
        *   `stock_finalization_queue`: For paid orders needing stock allocation.
        *   `order_fulfillment_queue`: For orders ready for fulfillment.
        *   `dead_order_queue`: Final DLQ for orders that cannot be processed.
    *   **Retry Queues:**
        *   `payment_retry_queue`: With delayed redelivery (e.g., using RabbitMQ delayed message exchange plugin or application-level logic) for payment retries.
    *   **Bindings:** Based on routing keys like `orders.global.for_payment`, `orders.global.for_stock`, etc.

5.  **Global Order Processors (Consumers):**
    *   All use manual acknowledgements and idempotency checks.
    *   **Payment Processor:**
        *   Consumes from `payment_processing_queue`.
        *   Integrates with payment gateway.
        *   On success, publishes to `global_orders_exchange` with routing key for stock finalization (e.g., `orders.global.for_stock`).
        *   On failure:
            *   If retryable, `nack` with requeue (or send to `payment_retry_queue`). Track retry count (e.g., in message header or idempotency store).
            *   After max retries (3), send to `dead_order_queue` (or a specific payment_failed_dlq).
        *   Updates idempotency store with payment status.
    *   **Stock Finalization Service:**
        *   Consumes from `stock_finalization_queue`.
        *   Atomically decrements stock in the `Stock Service`.
        *   If stock is unavailable, order is rejected (e.g., to `dead_order_queue` or a `stock_unavailable_queue`).
        *   On success, publishes to `global_orders_exchange` for fulfillment (e.g., `orders.global.for_fulfillment`).
        *   Updates idempotency store with stock allocation status.
    *   **Order Fulfillment Service:**
        *   Consumes from `order_fulfillment_queue`.
        *   Initiates fulfillment (e.g., notifies warehouse, sends confirmation email).
        *   Updates idempotency store with fulfillment status.

6.  **Stock Service:**
    *   Manages global item stock levels (e.g., in a transactional database or a system like Redis with Lua scripting for atomicity).
    *   Provides an atomic `decrementStock(itemId, quantity)` operation.
    *   Provides `getStockLevel(itemId)`.
    *   Must be highly consistent and available.

7.  **Idempotency/Deduplication & State Store:**
    *   A distributed database or cache (e.g., Redis, Cassandra, DynamoDB).
    *   Stores `orderId` and its processing state (e.g., `received_regional`, `payment_pending`, `payment_success`, `stock_allocated`, `fulfilled`, `cancelled`, `failed_reason`).
    *   Used by all consumers to ensure exactly-once processing and track retry counts.

8.  **Monitoring & Alerting System (e.g., Prometheus, Grafana, ELK Stack):**
    *   **RabbitMQ Metrics:** Queue lengths, message rates (in/out/ack/nack), consumer counts, connection status.
    *   **Application Metrics:** Processing times per step, error rates, retry counts, stock levels, payment success/failure rates, consumer health (CPU/memory).
    *   **Distributed Tracing:** To follow an order through the system.

9.  **(Bonus) Currency Conversion Service:**
    *   An independent service that can be called (e.g., async via RabbitMQ request/reply or gRPC) by Regional Consumers.
    *   Fetches and caches currency conversion rates.

10. **(Bonus) Order Cancellation Service:**
    *   Allows users to submit cancellation requests within 2 minutes.
    *   Cancellation requests are published to a high-priority queue (e.g., `cancellation_requests_queue`).
    *   Regional consumers or a dedicated cancellation processor check this. If an order is cancelled before processing starts, it's marked as such in the idempotency store and processing is skipped. This is a race condition that needs careful handling (e.g., check cancellation status *before* forwarding to global queue and *before* critical steps like payment).

## Key RabbitMQ Features & Strategies:

*   **Exchanges:** Topic for flexibility, Direct for specific routing if needed.
*   **Queues:** Durable, possibly Quorum Queues for HA.
*   **Acknowledgements:** Manual `ack/nack` by consumers.
*   **Publisher Confirms:** In Order Ingestion Service.
*   **Prefetch Count:** On consumers for backpressure.
*   **TTL:** For cancellation window messages, or for messages in retry queues if not using delayed exchange.
*   **Dead Letter Exchanges (DLX):** For unprocessable messages.
*   **Delayed Message Exchange Plugin:** For implementing retry delays without blocking consumers.
*   **Consistent Hashing Exchange:** Potentially for routing to `user_shard_queues` if native RabbitMQ sharding isn't used.

## Data Flow Example (Happy Path):

1.  User places order -> Order Ingestion Service.
2.  Ingestion Service validates, assigns IDs, publishes to `regional_orders_exchange` (e.g., `orders.user.shardX`).
3.  Regional Consumer picks up from `user_shard_queue_X`.
    *   Checks idempotency store (not processed). Marks as `processing_regional`.
    *   (Optional: currency conversion, cancellation check).
    *   Publishes to `global_orders_exchange` (e.g., `orders.global.for_payment`).
    *   `ack` message. Updates idempotency store.
4.  Payment Processor picks up from `payment_processing_queue`.
    *   Checks idempotency store. Marks as `payment_processing`.
    *   Calls payment gateway.
    *   On success, publishes to `global_orders_exchange` (e.g., `orders.global.for_stock`).
    *   `ack` message. Updates idempotency store to `payment_success`.
5.  Stock Finalization Service picks up from `stock_finalization_queue`.
    *   Checks idempotency store. Marks as `stock_processing`.
    *   Atomically decrements stock via Stock Service.
    *   On success, publishes to `global_orders_exchange` (e.g., `orders.global.for_fulfillment`).
    *   `ack` message. Updates idempotency store to `stock_allocated`.
6.  Order Fulfillment Service picks up from `order_fulfillment_queue`.
    *   Checks idempotency store. Marks as `fulfillment_processing`.
    *   Initiates fulfillment.
    *   `ack` message. Updates idempotency store to `fulfilled`.

## Handling Failures:

*   **Publisher fails to send to RabbitMQ:** Retry with backoff. Publisher Confirms detect this.
*   **Consumer Crash:** Unacknowledged message returns to queue. Another consumer picks it up. Idempotency store prevents reprocessing of already completed steps.
*   **Payment Failure:** Retry logic in Payment Processor using `payment_retry_queue`. After max retries, to `dead_order_queue`.
*   **Stock Unavailable:** Order to `dead_order_queue` or a specific `stock_unavailable_queue`.
*   **Downstream Service Unavailability:** `nack` and requeue (or send to retry queue with delay) if transient. If persistent, may need manual intervention or routing to DLQ.

This architecture aims to meet the requirements of reliability, ordering per user, exactly-once processing, stock management, resilience, scalability, and monitoring.
