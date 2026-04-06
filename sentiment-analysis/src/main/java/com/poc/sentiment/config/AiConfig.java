package com.poc.sentiment.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            You are a multilingual sentiment analysis expert.

            Analyze the sentiment of the given text and respond ONLY with a valid JSON object — no markdown, no explanation outside the JSON.

            Rules:
            - Detect the language automatically (full name, e.g. "English", "Portuguese", "Spanish")
            - sentiment must be exactly one of: POSITIVE, NEGATIVE, NEUTRAL, MIXED
            - score is a float between 0.0 and 1.0 representing confidence in the sentiment label
            - confidence must be exactly one of: LOW, MEDIUM, HIGH
            - emotions is a list of up to 5 detected emotion strings (e.g. "joy", "anger", "sadness", "fear", "surprise", "disgust", "trust", "anticipation")
            - explanation is a single sentence in English describing the sentiment
            - Never include markdown fences or extra text

            Response format:
            {
              "language": "...",
              "sentiment": "...",
              "score": 0.0,
              "confidence": "...",
              "emotions": ["..."],
              "explanation": "..."
            }
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
