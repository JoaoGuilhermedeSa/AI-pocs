package com.poc.gamediscussion.participant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.gamediscussion.model.ChatMessage;
import com.poc.gamediscussion.model.StreamEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OllamaParticipant implements AIParticipant {

    private final String id;
    private final String displayName;
    private final String model;
    private final String systemPrompt;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaParticipant(String id, String displayName, String model, String systemPrompt, String baseUrl) {
        this.id = id;
        this.displayName = displayName;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return displayName; }

    @Override
    public Flux<StreamEvent> chat(List<ChatMessage> history, String newMessage) {
        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", buildMessages(history, newMessage),
            "stream", true
        );

        return webClient.post()
            .uri("/api/chat")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToFlux(String.class)
            .filter(line -> !line.isBlank())
            .flatMap(this::parseChunk)
            .concatWith(Flux.just(new StreamEvent(id, displayName, "done", "")))
            .onErrorResume(e -> Flux.just(
                new StreamEvent(id, displayName, "error", "Ollama error: " + e.getMessage()),
                new StreamEvent(id, displayName, "done", "")
            ));
    }

    private List<Map<String, String>> buildMessages(List<ChatMessage> history, String newMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        // Group consecutive assistant messages to avoid invalid role sequences
        String pendingAssistant = null;
        for (ChatMessage msg : history) {
            if ("user".equals(msg.role())) {
                if (pendingAssistant != null) {
                    messages.add(Map.of("role", "assistant", "content", pendingAssistant));
                    pendingAssistant = null;
                }
                messages.add(Map.of("role", "user", "content", msg.content()));
            } else {
                String line = "[" + msg.participantName() + "]: " + msg.content();
                pendingAssistant = pendingAssistant == null ? line : pendingAssistant + "\n\n" + line;
            }
        }
        if (pendingAssistant != null) {
            messages.add(Map.of("role", "assistant", "content", pendingAssistant));
        }

        messages.add(Map.of("role", "user", "content", newMessage));
        return messages;
    }

    private Flux<StreamEvent> parseChunk(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.path("done").asBoolean(false)) return Flux.empty();
            JsonNode messageNode = node.get("message");
            if (messageNode != null) {
                String content = messageNode.path("content").asText("");
                if (!content.isEmpty()) {
                    return Flux.just(new StreamEvent(id, displayName, "token", content));
                }
            }
        } catch (Exception ignored) {}
        return Flux.empty();
    }
}
