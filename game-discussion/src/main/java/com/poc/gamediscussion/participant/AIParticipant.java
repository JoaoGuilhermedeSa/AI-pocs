package com.poc.gamediscussion.participant;

import com.poc.gamediscussion.model.ChatMessage;
import com.poc.gamediscussion.model.StreamEvent;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AIParticipant {
    String getId();
    String getDisplayName();
    Flux<StreamEvent> chat(List<ChatMessage> history, String newMessage);
}
