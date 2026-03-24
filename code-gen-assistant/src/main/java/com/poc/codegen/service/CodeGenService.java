package com.poc.codegen.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class CodeGenService {

    private final ChatClient chatClient;

    public CodeGenService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Streams generated code token-by-token for the given language and description.
     */
    public Flux<String> generate(String language, String description) {
        String prompt = """
                Language: %s

                Task: %s
                """.formatted(language, description);

        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }
}
