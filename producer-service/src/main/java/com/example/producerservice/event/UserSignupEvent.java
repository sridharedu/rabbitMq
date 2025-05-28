package com.example.producerservice.event;

import java.time.LocalDateTime;

// This POJO represents the data contract for user signup events.
// It's what gets serialized and sent over RabbitMQ.
// Using a clear, versioned POJO like this helps in maintaining compatibility
// between producer and consumer, even if they are updated independently.
public class UserSignupEvent {

    private String userId;
    private String email;
    private LocalDateTime timestamp;

    // Default constructor is often needed for JSON deserialization frameworks
    public UserSignupEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public UserSignupEvent(String userId, String email) {
        this.userId = userId;
        this.email = email;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "UserSignupEvent{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
