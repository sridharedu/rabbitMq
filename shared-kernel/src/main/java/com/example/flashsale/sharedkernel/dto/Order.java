package com.example.flashsale.sharedkernel.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal; // Using BigDecimal for currency potentially later
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @NotBlank(message = "Order ID cannot be blank")
    private String orderId;

    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    @NotBlank(message = "Item ID cannot be blank")
    private String itemId;

    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    // Consider if price/currency is needed here or handled later
    // For now, focusing on core requirements
    // private BigDecimal price;
    // private String currency;

    @NotNull(message = "Timestamp cannot be null")
    private Instant timestamp;
}
