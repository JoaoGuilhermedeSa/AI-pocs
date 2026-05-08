package com.poc.agent.tools;

import com.poc.agent.safety.AgentContext;
import com.poc.agent.safety.AgentTrace;
import com.poc.agent.safety.SafetyGuard;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ScratchpadTools {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> pads = new ConcurrentHashMap<>();
    private final AgentTrace trace;
    private final AgentContext agentContext;
    private final SafetyGuard safetyGuard;

    public ScratchpadTools(AgentTrace trace, AgentContext agentContext, SafetyGuard safetyGuard) {
        this.trace = trace;
        this.agentContext = agentContext;
        this.safetyGuard = safetyGuard;
    }

    @Tool(description = "Save a finding or piece of information under a short key for later use. Use descriptive keys like 'market_size', 'key_quote', 'source_1'.")
    public String saveNote(String key, String content) {
        safetyGuard.checkIterations();
        var convId = agentContext.getConversationId();
        trace.log(convId, "saveNote", "Saved note: " + key);
        pad(convId).put(key, content);
        return "Note saved under key: " + key;
    }

    @Tool(description = "Read a previously saved note by its key.")
    public String readNote(String key) {
        safetyGuard.checkIterations();
        var convId = agentContext.getConversationId();
        var note = pad(convId).get(key);
        return note != null ? note : "No note found for key: " + key;
    }

    @Tool(description = "List all saved note keys for the current session.")
    public String listNotes() {
        safetyGuard.checkIterations();
        var convId = agentContext.getConversationId();
        var keys = pad(convId).keySet();
        return keys.isEmpty() ? "No notes saved yet." : "Saved notes: " + String.join(", ", keys);
    }

    public void clearForConversation(String conversationId) {
        pads.remove(conversationId);
    }

    private ConcurrentHashMap<String, String> pad(String conversationId) {
        return pads.computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>());
    }
}
