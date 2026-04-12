package com.poc.moderation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.moderation.dto.ModerationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class TextModerationService {

    private static final Logger log = LoggerFactory.getLogger(TextModerationService.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TextModerationService(@Qualifier("textChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Mono<ModerationResponse> moderate(String text) {
        return Mono.fromCallable(() -> {
            String raw = chatClient.prompt()
                    .user(text)
                    .call()
                    .content();
            log.debug("Text moderation raw output: {}", raw);
            return parse(raw);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    ModerationResponse parse(String raw) throws Exception {
        String cleaned = raw.replaceAll("(?s)<think>.*?</think>", "").trim();
        cleaned = cleaned.replaceAll("(?s)```[a-z]*\\s*", "").replace("```", "").trim();
        return objectMapper.readValue(cleaned, ModerationResponse.class);
    }
}
