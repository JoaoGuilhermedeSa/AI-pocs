package com.poc.codegen.controller;

import com.poc.codegen.dto.CodeGenRequest;
import com.poc.codegen.service.CodeGenService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * REST + SSE endpoints for the code generation assistant.
 *
 * POST /api/codegen/stream  → SSE stream of tokens (events: token | done | error)
 * GET  /api/codegen/languages → list of supported languages
 */
@RestController
@RequestMapping("/api/codegen")
public class CodeGenController {

    private final CodeGenService codeGenService;

    public CodeGenController(CodeGenService codeGenService) {
        this.codeGenService = codeGenService;
    }

    /**
     * Streams the generated code as Server-Sent Events.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestBody CodeGenRequest request) {
        return codeGenService.generate(request.language(), request.description())
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
     * Returns the list of supported languages shown in the UI.
     */
    @GetMapping("/languages")
    public Flux<String> languages() {
        return Flux.just(
                "Java", "Python", "JavaScript", "TypeScript",
                "Go", "Rust", "C#", "C++",
                "Kotlin", "Swift", "SQL", "Bash"
        );
    }
}
