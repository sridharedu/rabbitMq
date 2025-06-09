package com.example.flashsale.sharedkernel.config;

public final class RabbitMQConfigConstants {

    private RabbitMQConfigConstants() {
        // Private constructor to prevent instantiation
    }

    // Exchanges
    public static final String REGIONAL_ORDERS_EXCHANGE = "regional_orders_exchange";
    // Potentially: public static final String GLOBAL_ORDERS_EXCHANGE = "global_orders_exchange";

    // Dead Letter Exchanges
    public static final String REGIONAL_ORDERS_DLX = "regional_orders_dlx";

    // Queue name patterns/prefixes - actual names will be composed
    public static final String USER_SHARD_QUEUE_NAME_PREFIX = "user_shard_queue_"; // e.g., user_shard_queue_0
    public static final String REGIONAL_ORDERS_DEAD_LETTER_QUEUE_NAME = "regional_orders_dead_letter_queue";

    // Routing key patterns
    public static final String ORDER_ROUTING_KEY_PREFIX = "orders.user.shard"; // e.g., orders.user.shard0
    public static final String COMMON_DLQ_ROUTING_KEY = "deadletter.regional";


    // Number of shards for user queues (can be externalized to config later)
    public static final int NUM_USER_SHARDS = 10; // Example value

    public static String getUserShardQueueName(int shardId) {
        return USER_SHARD_QUEUE_NAME_PREFIX + shardId;
    }

    public static String getOrderRoutingKey(int shardId) {
        return ORDER_ROUTING_KEY_PREFIX + shardId;
    }
}
