package com.poc.assistant.controller;

import com.poc.assistant.model.ChatRequest;
import com.poc.assistant.service.AssistantService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody ChatRequest request) {
        var conversationId = (request.conversationId() != null && !request.conversationId().isBlank())
                ? request.conversationId()
                : UUID.randomUUID().toString();

        return assistantService.chat(request.message(), conversationId)
                .map(token -> ServerSentEvent.<String>builder()
                        .event("token").data(token).build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done").data(conversationId).build()))
                .onErrorResume(ex -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error").data(ex.getMessage()).build()));
    }
}
