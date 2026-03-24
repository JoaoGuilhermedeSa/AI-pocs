package com.poc.codegen.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            You are an expert software engineer and code generation assistant.

            When given a natural language description, generate clean, idiomatic, production-quality code.

            Rules:
            - Start with a one-line comment explaining what the code does
            - Include all necessary imports
            - Follow best practices and idiomatic style for the requested language
            - Add brief inline comments for non-obvious logic
            - If the request is ambiguous, make a reasonable assumption and note it in a comment
            - Return the code inside a markdown fenced code block with the language tag
            - After the code block, add a short "Notes:" section with any important caveats or usage tips
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
