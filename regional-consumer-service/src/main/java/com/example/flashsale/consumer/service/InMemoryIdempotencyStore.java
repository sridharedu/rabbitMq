package com.example.flashsale.consumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.Duration; // Added import
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryIdempotencyStore.class);

    private static class OrderState {
        ProcessingStatus status;
        Instant timestamp;

        OrderState(ProcessingStatus status, Instant timestamp) {
            this.status = status;
            this.timestamp = timestamp;
        }
    }

    private enum ProcessingStatus {
        PROCESSING,
        PROCESSED
    }

    private final ConcurrentHashMap<String, OrderState> store = new ConcurrentHashMap<>();

    @Value("${flashsale.idempotency.processing.timeout-seconds:60}")
    private long processingTimeoutSeconds;

    @Override
    @PostConstruct // Or a manual call if preferred for testing environments
    public void initialize() {
        store.clear();
        log.info("In-memory Idempotency Store initialized (cleared). Processing timeout: {}s", processingTimeoutSeconds);
    }

    @Override
    public ProcessingState checkAndSetProcessing(String orderId) {
        Instant now = Instant.now();
        // Simpler approach for checkAndSet:
        OrderState state = store.get(orderId);
        if (state != null) {
            if (state.status == ProcessingStatus.PROCESSED) {
                log.debug("Order ID '{}' already PROCESSED at {}.", orderId, state.timestamp);
                return ProcessingState.ALREADY_PROCESSED;
            }
            // It's PROCESSING
            if (Duration.between(state.timestamp, now).getSeconds() > processingTimeoutSeconds) {
                log.warn("Order ID '{}' had stale PROCESSING mark from {}. Attempting to acquire.", orderId, state.timestamp);
                // Attempt to atomically update if it's still the same stale entry
                OrderState updatedState = new OrderState(ProcessingStatus.PROCESSING, now);
                if (store.replace(orderId, state, updatedState)) {
                     log.info("Order ID '{}' acquired stale PROCESSING mark. Now PROCESSING by current.", orderId);
                    return ProcessingState.OK_TO_PROCESS;
                } else {
                    // Lost race, another thread/instance updated it. Re-evaluate.
                    log.debug("Order ID '{}' state changed during stale check. Re-evaluating.", orderId);
                    return checkAndSetProcessing(orderId); // Recurse or loop for robust check
                }
            } else {
                log.debug("Order ID '{}' currently PROCESSING by other (since {}).", orderId, state.timestamp);
                return ProcessingState.CURRENTLY_PROCESSING;
            }
        } else {
            // Try to put new processing mark
            OrderState newState = new OrderState(ProcessingStatus.PROCESSING, now);
            OrderState previous = store.putIfAbsent(orderId, newState);
            if (previous == null) {
                log.info("Order ID '{}' new. Marked as PROCESSING.", orderId);
                return ProcessingState.OK_TO_PROCESS;
            } else {
                // Lost race, another thread/instance just marked it. Re-evaluate.
                 log.debug("Order ID '{}' was concurrently marked. Re-evaluating.", orderId);
                return checkAndSetProcessing(orderId); // Recurse or loop
            }
        }
    }


    @Override
    public void markAsProcessed(String orderId) {
        store.put(orderId, new OrderState(ProcessingStatus.PROCESSED, Instant.now()));
        log.info("Order ID '{}' marked as PROCESSED.", orderId);
    }

    @Override
    public void clearProcessingMark(String orderId) {
        store.computeIfPresent(orderId, (key, existingState) -> {
            if (existingState.status == ProcessingStatus.PROCESSING) {
                log.info("Order ID '{}' clearing PROCESSING mark.", key);
                return null; // Remove the entry
            }
            return existingState; // Otherwise, leave it (e.g., if it became PROCESSED)
        });
    }
}
