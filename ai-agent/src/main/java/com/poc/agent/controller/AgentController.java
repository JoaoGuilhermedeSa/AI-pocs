package com.poc.agent.controller;

import com.poc.agent.model.ChatRequest;
import com.poc.agent.model.TraceEntry;
import com.poc.agent.safety.AgentTrace;
import com.poc.agent.service.AgentService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;
    private final AgentTrace agentTrace;

    public AgentController(AgentService agentService, AgentTrace agentTrace) {
        this.agentService = agentService;
        this.agentTrace = agentTrace;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody ChatRequest request) {
        var conversationId = (request.conversationId() != null && !request.conversationId().isBlank())
                ? request.conversationId()
                : UUID.randomUUID().toString();

        return agentService.chat(request.message(), conversationId)
                .map(token -> ServerSentEvent.<String>builder()
                        .event("token").data(token).build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done").data(conversationId).build()))
                .onErrorResume(ex -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error").data(ex.getMessage()).build()));
    }

    @GetMapping("/trace/{conversationId}")
    public List<TraceEntry> trace(@PathVariable String conversationId) {
        return agentTrace.get(conversationId);
    }
}
