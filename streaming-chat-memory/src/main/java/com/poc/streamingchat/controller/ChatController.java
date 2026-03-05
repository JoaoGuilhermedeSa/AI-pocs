package com.poc.streamingchat.controller;

import com.poc.streamingchat.dto.ChatRequest;
import com.poc.streamingchat.dto.HistoryResponse;
import com.poc.streamingchat.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST + SSE endpoints for the chat POC.
 *
 * POST   /api/chat/stream              → SSE stream of tokens
 * GET    /api/chat/history/{id}        → full message history
 * DELETE /api/chat/{id}               → clear conversation
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Streams the AI response as Server-Sent Events.
     *
     * SSE event types:
     *   - token  : one text fragment from the model
     *   - done   : signals the stream has ended (data is empty)
     *   - error  : signals an error (data contains the message)
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody ChatRequest request) {
        return chatService.streamChat(request.message(), request.conversationId())
                .map(token -> ServerSentEvent.<String>builder()
                        .event("token")
                        .data(token)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ))
                .onErrorResume(ex -> Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("error")
                                .data(ex.getMessage() != null ? ex.getMessage() : "Unknown error")
                                .build()
                ));
    }

    /**
     * Returns the stored message history for a conversation.
     */
    @GetMapping("/history/{conversationId}")
    public Mono<HistoryResponse> getHistory(@PathVariable String conversationId) {
        return Mono.fromCallable(() -> chatService.getHistory(conversationId));
    }

    /**
     * Clears all messages for the given conversation.
     */
    @DeleteMapping("/{conversationId}")
    public Mono<Void> clearConversation(@PathVariable String conversationId) {
        return Mono.fromRunnable(() -> chatService.clearConversation(conversationId));
    }
}
