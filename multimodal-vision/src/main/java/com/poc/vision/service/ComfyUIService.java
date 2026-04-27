package com.poc.vision.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.vision.config.VisionProperties;
import com.poc.vision.model.GenerateRequest;
import com.poc.vision.model.StreamEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
public class ComfyUIService {

    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final String workflowTemplate;

    public ComfyUIService(VisionProperties props, ObjectMapper mapper) throws Exception {
        this.webClient = WebClient.create(props.getComfyui().getUrl());
        this.mapper = mapper;
        this.workflowTemplate = new ClassPathResource("flux-workflow.json")
            .getContentAsString(StandardCharsets.UTF_8);
    }

    public Flux<StreamEvent> generate(GenerateRequest req) {
        String clientId = UUID.randomUUID().toString();

        return Mono.fromCallable(() -> buildPrompt(req, clientId))
            .flatMap(promptBody -> webClient.post()
                .uri("/prompt")
                .bodyValue(promptBody)
                .retrieve()
                .bodyToMono(Map.class))
            .flatMapMany(response -> {
                String promptId = (String) response.get("prompt_id");
                if (promptId == null) return Flux.just(new StreamEvent("error", "ComfyUI did not return a prompt_id"));
                return Flux.concat(
                    Flux.just(new StreamEvent("progress", "Generating… (prompt_id: " + promptId + ")")),
                    pollUntilDone(promptId)
                );
            })
            .onErrorResume(e -> Flux.just(new StreamEvent("error", e.getMessage())));
    }

    private Flux<StreamEvent> pollUntilDone(String promptId) {
        return Flux.interval(Duration.ofSeconds(2))
            .flatMap(tick -> webClient.get()
                .uri("/history/" + promptId)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorReturn(Map.of()))
            .flatMap(history -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) history.get(promptId);
                if (entry == null) return Flux.<StreamEvent>empty();
                String imageUrl = extractImageUrl(entry);
                if (imageUrl == null) return Flux.<StreamEvent>empty();
                return Flux.just(
                    new StreamEvent("progress", "Done!"),
                    new StreamEvent("image", imageUrl)
                );
            })
            .takeUntil(e -> "image".equals(e.type()))
            .timeout(Duration.ofMinutes(5))
            .onErrorResume(e -> Flux.just(new StreamEvent("error", "Timed out waiting for ComfyUI")));
    }

    @SuppressWarnings("unchecked")
    private String extractImageUrl(Map<String, Object> entry) {
        try {
            Map<String, Object> outputs = (Map<String, Object>) entry.get("outputs");
            if (outputs == null) return null;
            for (Object nodeOutput : outputs.values()) {
                Map<String, Object> out = (Map<String, Object>) nodeOutput;
                var images = (java.util.List<Map<String, Object>>) out.get("images");
                if (images != null && !images.isEmpty()) {
                    String filename = (String) images.getFirst().get("filename");
                    return "/api/vision/output/" + filename;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Map<String, Object> buildPrompt(GenerateRequest req, String clientId) throws Exception {
        String workflow = workflowTemplate
            .replace("\"PROMPT_PLACEHOLDER\"", mapper.writeValueAsString(req.prompt()))
            .replace("\"SEED_PLACEHOLDER\"", String.valueOf(req.seed()))
            .replace("\"WIDTH_PLACEHOLDER\"", String.valueOf(req.width()))
            .replace("\"HEIGHT_PLACEHOLDER\"", String.valueOf(req.height()))
            .replace("\"STEPS_PLACEHOLDER\"", String.valueOf(req.steps()));

        Map<String, Object> workflowMap = mapper.readValue(workflow, new TypeReference<>() {});
        return Map.of("prompt", workflowMap, "client_id", clientId);
    }
}
