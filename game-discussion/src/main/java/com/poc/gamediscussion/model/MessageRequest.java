package com.poc.gamediscussion.model;

import java.util.List;

public record MessageRequest(
    String sessionId,
    String message,
    List<String> participantIds
) {}
