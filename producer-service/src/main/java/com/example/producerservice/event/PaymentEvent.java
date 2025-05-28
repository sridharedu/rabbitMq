package com.example.producerservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// This POJO represents the data contract for payment events.
// It's serialized and sent over RabbitMQ to the 'events.topic' exchange.
// Different payment types can be routed using keys like 'payment.card', 'payment.wallet', etc.
public class PaymentEvent {

    private String paymentId;
    private BigDecimal amount;
    private String currency;
    private String paymentType; // e.g., "card", "wallet", "bank_transfer"
    private LocalDateTime timestamp;

    // Default constructor
    public PaymentEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public PaymentEvent(String paymentId, BigDecimal amount, String currency, String paymentType) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
        this.paymentType = paymentType;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PaymentEvent{" +
                "paymentId='" + paymentId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", paymentType='" + paymentType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
