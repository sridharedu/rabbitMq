package com.example.consumerservice.event;

import java.time.LocalDateTime;

// This POJO represents the data contract for received user signup events.
// It must match the structure of the UserSignupEvent produced by the producer-service
// for successful JSON deserialization by Spring AMQP.
public class UserSignupEvent {

    private String userId;
    private String email;
    private LocalDateTime timestamp;

    // Default constructor is essential for Jackson deserialization
    public UserSignupEvent() {
    }

    // Getters and Setters are also essential for Jackson
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
