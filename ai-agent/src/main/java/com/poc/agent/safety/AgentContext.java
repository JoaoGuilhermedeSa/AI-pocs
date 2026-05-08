package com.poc.agent.safety;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AgentContext {

    private volatile String currentConversationId = "";
    private final ConcurrentHashMap<String, AtomicInteger> iterationCounts = new ConcurrentHashMap<>();

    public void start(String conversationId) {
        this.currentConversationId = conversationId;
        iterationCounts.put(conversationId, new AtomicInteger(0));
    }

    public String getConversationId() {
        return currentConversationId;
    }

    public int incrementAndGetIterations() {
        return iterationCounts
                .computeIfAbsent(currentConversationId, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    public void clear(String conversationId) {
        iterationCounts.remove(conversationId);
    }
}
