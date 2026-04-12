package com.poc.moderation.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    public static final String TEXT_SYSTEM_PROMPT = """
            You are a content moderation expert. Analyze the given text for harmful content.

            Respond ONLY with a valid JSON object — no markdown, no extra text.

            Categories to detect: HATE_SPEECH, VIOLENCE, ADULT, SPAM, SELF_HARM, HARASSMENT
            If no harmful content: use categories ["NONE"]

            Response format:
            {
              "verdict": "SAFE" or "UNSAFE",
              "categories": ["..."],
              "severity": "NONE" or "LOW" or "MEDIUM" or "HIGH",
              "confidence": 0.0,
              "explanation": "One sentence in English."
            }
            """;

    public static final String VISION_SYSTEM_PROMPT = """
            You are a visual content moderation expert. Analyze the given image for harmful visual content.

            Respond ONLY with a valid JSON object — no markdown, no extra text.

            Categories to detect: HATE_SPEECH, VIOLENCE, ADULT, SPAM, SELF_HARM, HARASSMENT
            If no harmful content: use categories ["NONE"]

            Response format:
            {
              "verdict": "SAFE" or "UNSAFE",
              "categories": ["..."],
              "severity": "NONE" or "LOW" or "MEDIUM" or "HIGH",
              "confidence": 0.0,
              "explanation": "One sentence in English."
            }
            """;

    @Value("${moderation.models.text}")
    private String textModel;

    @Bean("textChatClient")
    public ChatClient textChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(TEXT_SYSTEM_PROMPT)
                .defaultOptions(org.springframework.ai.chat.prompt.ChatOptions.builder()
                        .model(textModel)
                        .temperature(0.1)
                        .build())
                .build();
    }

}
