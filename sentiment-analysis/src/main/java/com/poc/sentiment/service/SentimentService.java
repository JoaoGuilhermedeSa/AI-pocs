package com.poc.sentiment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.sentiment.dto.SentimentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class SentimentService {

    private static final Logger log = LoggerFactory.getLogger(SentimentService.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SentimentService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Mono<SentimentResponse> analyze(String text) {
        return Mono.fromCallable(() -> {
            String raw = chatClient.prompt()
                    .user(text)
                    .call()
                    .content();
            return parse(raw);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private SentimentResponse parse(String raw) throws Exception {
        log.info("RAW MODEL OUTPUT: >>{}<<", raw);
        // Strip <think>...</think> blocks produced by qwen3 reasoning mode
        String cleaned = raw.replaceAll("(?s)<think>.*?</think>", "").trim();
        // Strip markdown fences (```json ... ``` or ``` ... ```)
        cleaned = cleaned.replaceAll("(?s)```[a-z]*\\s*", "").replace("```", "").trim();
        log.info("CLEANED OUTPUT: >>{}<<", cleaned);
        return objectMapper.readValue(cleaned, SentimentResponse.class);
    }

    public Flux<SentimentResponse> analyzeBatch(java.util.List<String> texts) {
        return Flux.fromIterable(texts)
                .flatMapSequential(this::analyze);
    }
}
