package com.poc.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant that answers questions based strictly on the provided context documents.

            Rules:
            - Answer only from the context provided. Do not use outside knowledge.
            - If the context does not contain enough information, say so clearly.
            - Be concise and factual.
            - When relevant, mention which document or section your answer comes from.
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
