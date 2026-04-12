package com.poc.moderation.dto;

import java.util.List;

/**
 * Aggregated result for video moderation (visual frames + audio transcript).
 */
public record VideoModerationResponse(
        String verdict,
        List<String> categories,
        String severity,
        double confidence,
        String explanation,
        ModerationResponse visualResult,
        ModerationResponse audioResult,
        int framesAnalyzed
) {}
