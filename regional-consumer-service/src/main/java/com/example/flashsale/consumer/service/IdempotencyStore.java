package com.example.flashsale.consumer.service;

public interface IdempotencyStore {

    enum ProcessingState {
        OK_TO_PROCESS,       // Order is new or stale, safe to process
        ALREADY_PROCESSED,   // Order has been successfully processed before
        CURRENTLY_PROCESSING // Order is likely being processed by another transaction/thread
    }

    ProcessingState checkAndSetProcessing(String orderId);
    void markAsProcessed(String orderId);
    void clearProcessingMark(String orderId);
    void initialize(); // For clearing the store during startup if needed for testing
}
