package com.example.flashsale.consumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InMemoryStockService implements StockService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryStockService.class);
    private final ConcurrentHashMap<String, AtomicInteger> stockLevels = new ConcurrentHashMap<>();

    @PostConstruct
    public void setupInitialStock() {
        // Initialize with some default stock for demonstration
        initializeStock("item_1", 10);
        initializeStock("item_2", 5);
        initializeStock("item_3", 20);
        initializeStock("item_A", 0); // Item with no stock
        log.info("In-Memory Stock Service initialized with default stock.");
    }

    @Override
    public void initializeStock(String itemId, int quantity) {
        stockLevels.put(itemId, new AtomicInteger(quantity));
        log.info("Stock initialized for item {}: {}", itemId, quantity);
    }

    @Override
    public void resetStock() {
        stockLevels.clear();
        log.info("All stock cleared from In-Memory Stock Service.");
        // Optionally re-run setupInitialStock if a default state is always desired after reset
        // setupInitialStock();
    }


    @Override
    public boolean decreaseStock(String itemId, int quantityToDecrease) {
        if (quantityToDecrease <= 0) {
            log.warn("Attempted to decrease stock for item {} with non-positive quantity: {}", itemId, quantityToDecrease);
            return false;
        }

        AtomicInteger currentStock = stockLevels.get(itemId);
        if (currentStock == null) {
            log.warn("Item {} not found in stock.", itemId);
            return false; // Item not found
        }

        // Loop for atomic CAS (Compare-And-Swap) operation
        while (true) {
            int availableStock = currentStock.get();
            if (availableStock < quantityToDecrease) {
                log.warn("Insufficient stock for item {}. Requested: {}, Available: {}", itemId, quantityToDecrease, availableStock);
                return false; // Not enough stock
            }
            // Try to decrease stock atomically
            if (currentStock.compareAndSet(availableStock, availableStock - quantityToDecrease)) {
                log.info("Stock for item {} decreased by {}. New stock: {}", itemId, quantityToDecrease, availableStock - quantityToDecrease);
                return true; // Successfully decreased
            }
            // If CAS failed, it means another thread updated the stock. Loop will retry.
            log.debug("CAS failed for item {}. Retrying stock decrease.", itemId);
        }
    }

    @Override
    public int getStockLevel(String itemId) {
        AtomicInteger stock = stockLevels.get(itemId);
        return (stock != null) ? stock.get() : 0;
    }
}
