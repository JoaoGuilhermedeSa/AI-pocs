package com.poc.sentiment.dto;

import java.util.List;

/**
 * Structured sentiment analysis result.
 *
 * sentiment: POSITIVE | NEGATIVE | NEUTRAL | MIXED
 * score: 0.0–1.0 confidence in the sentiment label
 * confidence: LOW | MEDIUM | HIGH
 */
public record SentimentResponse(
        String language,
        String sentiment,
        double score,
        String confidence,
        List<String> emotions,
        String explanation
) {}
