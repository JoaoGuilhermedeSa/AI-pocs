package com.poc.gamediscussion.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiscussionSession {
    private final String id;
    private final Instant createdAt = Instant.now();
    private final List<ChatMessage> messages = new ArrayList<>();

    public DiscussionSession(String id) {
        this.id = id;
    }

    public synchronized void addMessage(ChatMessage message) {
        messages.add(message);
    }

    public synchronized List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public String getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
}
