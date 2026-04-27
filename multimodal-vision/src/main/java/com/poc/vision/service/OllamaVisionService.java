package com.poc.vision.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.vision.config.VisionProperties;
import com.poc.vision.model.StreamEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Map;

@Service
public class OllamaVisionService {

    private final VisionProperties.Ollama config;
    private final WebClient webClient;
    private final ObjectMapper mapper;

    public OllamaVisionService(VisionProperties props, ObjectMapper mapper) {
        this.config = props.getOllama();
        this.webClient = WebClient.create(config.getUrl());
        this.mapper = mapper;
    }

    public Flux<StreamEvent> analyze(String base64Image, String prompt) {
        var body = Map.of(
            "model", config.getModel(),
            "stream", true,
            "messages", List.of(Map.of(
                "role", "user",
                "content", prompt,
                "images", List.of(base64Image)
            ))
        );

        return webClient.post()
            .uri("/api/chat")
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(String.class)
            .flatMap(line -> {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> json = mapper.readValue(line, Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> msg = (Map<String, Object>) json.get("message");
                    Boolean done = (Boolean) json.get("done");
                    if (Boolean.TRUE.equals(done)) return Flux.just(new StreamEvent("done", ""));
                    if (msg != null) {
                        String token = (String) msg.get("content");
                        if (token != null && !token.isEmpty()) return Flux.just(new StreamEvent("token", token));
                    }
                    return Flux.empty();
                } catch (Exception e) {
                    return Flux.empty();
                }
            })
            .onErrorResume(e -> Flux.just(new StreamEvent("error", e.getMessage())));
    }
}
