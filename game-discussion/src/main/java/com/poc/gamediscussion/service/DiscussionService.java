package com.poc.gamediscussion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.gamediscussion.model.ChatMessage;
import com.poc.gamediscussion.model.DiscussionSession;
import com.poc.gamediscussion.model.MessageRequest;
import com.poc.gamediscussion.model.StreamEvent;
import com.poc.gamediscussion.participant.AIParticipant;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DiscussionService {

    private final List<AIParticipant> participants;
    private final Map<String, DiscussionSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DiscussionService(List<AIParticipant> participants) {
        this.participants = participants;
    }

    public DiscussionSession getOrCreateSession(String sessionId) {
        return sessions.computeIfAbsent(sessionId, DiscussionSession::new);
    }

    public DiscussionSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public List<Map<String, String>> getParticipantInfo() {
        return participants.stream()
            .map(p -> Map.of("id", p.getId(), "name", p.getDisplayName()))
            .toList();
    }

    public Flux<ServerSentEvent<String>> processMessage(MessageRequest request) {
        DiscussionSession session = getOrCreateSession(request.sessionId());

        List<ChatMessage> historyBeforeThisMessage = session.getMessages();
        session.addMessage(new ChatMessage("user", null, "You", request.message(), Instant.now()));

        List<AIParticipant> selected = request.participantIds() == null || request.participantIds().isEmpty()
            ? participants
            : participants.stream().filter(p -> request.participantIds().contains(p.getId())).toList();

        Map<String, StringBuilder> accumulators = new ConcurrentHashMap<>();

        List<Flux<StreamEvent>> streams = selected.stream()
            .map(p -> p.chat(historyBeforeThisMessage, request.message())
                .doOnNext(event -> {
                    if ("token".equals(event.type())) {
                        accumulators.computeIfAbsent(event.participantId(), k -> new StringBuilder())
                            .append(event.content());
                    } else if ("done".equals(event.type())) {
                        String content = accumulators.getOrDefault(event.participantId(), new StringBuilder()).toString();
                        if (!content.isBlank()) {
                            session.addMessage(new ChatMessage(
                                "assistant", event.participantId(), event.participantName(),
                                content, Instant.now()
                            ));
                        }
                    }
                })
            )
            .toList();

        return Flux.merge(streams)
            .map(this::toSse);
    }

    private ServerSentEvent<String> toSse(StreamEvent event) {
        try {
            return ServerSentEvent.<String>builder()
                .data(objectMapper.writeValueAsString(event))
                .build();
        } catch (JsonProcessingException e) {
            return ServerSentEvent.<String>builder()
                .data("{\"type\":\"error\",\"content\":\"serialization error\"}")
                .build();
        }
    }
}
