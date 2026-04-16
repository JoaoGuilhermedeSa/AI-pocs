package com.poc.gamediscussion.model;

public record StreamEvent(
    String participantId,
    String participantName,
    String type,
    String content
) {}
