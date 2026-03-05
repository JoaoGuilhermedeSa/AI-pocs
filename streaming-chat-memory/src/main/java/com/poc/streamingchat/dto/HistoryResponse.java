package com.poc.streamingchat.dto;

import java.util.List;

/**
 * Full conversation history for a given conversationId.
 */
public record HistoryResponse(String conversationId, List<MessageDto> messages) {}
