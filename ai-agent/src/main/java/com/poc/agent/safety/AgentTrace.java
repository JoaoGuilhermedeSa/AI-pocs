package com.poc.agent.safety;

import com.poc.agent.model.TraceEntry;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentTrace {

    private final ConcurrentHashMap<String, List<TraceEntry>> traces = new ConcurrentHashMap<>();

    public void log(String conversationId, String tool, String description) {
        traces.computeIfAbsent(conversationId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new TraceEntry(tool, description, Instant.now()));
    }

    public List<TraceEntry> get(String conversationId) {
        return traces.getOrDefault(conversationId, List.of());
    }

    public void clear(String conversationId) {
        traces.remove(conversationId);
    }
}
