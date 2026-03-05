package com.poc.streamingchat.dto;

/**
 * Incoming chat request body.
 * Compact constructor validates non-blank fields.
 */
public record ChatRequest(String message, String conversationId) {

    public ChatRequest {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("'message' must not be blank");
        }
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("'conversationId' must not be blank");
        }
    }
}
