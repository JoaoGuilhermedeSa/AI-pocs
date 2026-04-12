package com.poc.moderation.dto;

import java.util.List;

/**
 * verdict:    SAFE | UNSAFE
 * categories: HATE_SPEECH | VIOLENCE | ADULT | SPAM | SELF_HARM | HARASSMENT | NONE
 * severity:   LOW | MEDIUM | HIGH | NONE
 * confidence: 0.0–1.0
 */
public record ModerationResponse(
        String verdict,
        List<String> categories,
        String severity,
        double confidence,
        String explanation
) {}
