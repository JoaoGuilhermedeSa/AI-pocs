package com.poc.gamediscussion.controller;

import com.poc.gamediscussion.model.ChatMessage;
import com.poc.gamediscussion.model.DiscussionSession;
import com.poc.gamediscussion.model.MessageRequest;
import com.poc.gamediscussion.service.DiscussionService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/discussion")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @PostMapping(value = "/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sendMessage(@RequestBody MessageRequest request) {
        return discussionService.processMessage(request);
    }

    @GetMapping("/participants")
    public Mono<List<Map<String, String>>> getParticipants() {
        return Mono.just(discussionService.getParticipantInfo());
    }

    @PostMapping("/sessions")
    public Mono<Map<String, String>> createSession() {
        String id = UUID.randomUUID().toString();
        discussionService.getOrCreateSession(id);
        return Mono.just(Map.of("sessionId", id));
    }

    @GetMapping("/sessions/{id}")
    public Mono<Map<String, Object>> getSession(@PathVariable String id) {
        DiscussionSession session = discussionService.getSession(id);
        if (session == null) return Mono.empty();
        return Mono.just(Map.of(
            "id", session.getId(),
            "messages", session.getMessages(),
            "createdAt", session.getCreatedAt()
        ));
    }
}
