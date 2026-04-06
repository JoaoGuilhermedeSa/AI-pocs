package com.poc.sentiment.controller;

import com.poc.sentiment.dto.BatchRequest;
import com.poc.sentiment.dto.SentimentRequest;
import com.poc.sentiment.dto.SentimentResponse;
import com.poc.sentiment.service.SentimentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * POST /api/sentiment/analyze → single text analysis
 * POST /api/sentiment/batch   → list of texts, returns Flux of results
 */
@RestController
@RequestMapping("/api/sentiment")
public class SentimentController {

    private final SentimentService sentimentService;

    public SentimentController(SentimentService sentimentService) {
        this.sentimentService = sentimentService;
    }

    @PostMapping("/analyze")
    public Mono<SentimentResponse> analyze(@RequestBody SentimentRequest request) {
        return sentimentService.analyze(request.text());
    }

    @PostMapping("/batch")
    public Flux<SentimentResponse> batch(@RequestBody BatchRequest request) {
        return sentimentService.analyzeBatch(request.texts());
    }
}
