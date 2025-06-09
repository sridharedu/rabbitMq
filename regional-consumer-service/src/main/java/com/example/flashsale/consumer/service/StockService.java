package com.example.flashsale.consumer.service;

public interface StockService {
    boolean decreaseStock(String itemId, int quantity);
    int getStockLevel(String itemId);
    void initializeStock(String itemId, int quantity); // For setting up initial stock
    void resetStock(); // For testing, to clear all stock
}
