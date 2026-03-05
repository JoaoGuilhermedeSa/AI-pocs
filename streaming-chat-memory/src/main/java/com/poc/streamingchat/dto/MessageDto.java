package com.poc.streamingchat.dto;

import java.time.Instant;

/**
 * A single chat message for the history API.
 */
public record MessageDto(String role, String content, Instant timestamp) {}
