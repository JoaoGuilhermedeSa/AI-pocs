package com.poc.gamediscussion.model;

import java.time.Instant;

public record ChatMessage(
    String role,
    String participantId,
    String participantName,
    String content,
    Instant timestamp
) {}
