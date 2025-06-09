package com.example.flashsale.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry; // For later Spring Retry use

@SpringBootApplication
@EnableRetry // Enable Spring Retry capabilities for later steps
public class RegionalConsumerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RegionalConsumerServiceApplication.class, args);
    }
}
