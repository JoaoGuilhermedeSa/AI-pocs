package com.poc.streamingchat.service;

import com.poc.streamingchat.dto.HistoryResponse;
import com.poc.streamingchat.dto.MessageDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final InMemoryChatMemoryRepository memoryRepository;

    public ChatService(ChatClient chatClient, InMemoryChatMemoryRepository memoryRepository) {
        this.chatClient = chatClient;
        this.memoryRepository = memoryRepository;
    }

    /**
     * Streams the assistant response token-by-token.
     * The MessageChatMemoryAdvisor automatically loads prior messages
     * for the given conversationId and saves both the user message and
     * the completed assistant reply back to the repository.
     */
    public Flux<String> streamChat(String message, String conversationId) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    /**
     * Returns the full message history stored for a conversation.
     */
    public HistoryResponse getHistory(String conversationId) {
        List<Message> messages = memoryRepository.findByConversationId(conversationId);
        List<MessageDto> dtos = messages.stream()
                .map(this::toDto)
                .toList();
        return new HistoryResponse(conversationId, dtos);
    }

    /**
     * Wipes all messages for a conversation — useful for "New Chat" resets
     * that go through the backend, or for the DELETE endpoint.
     */
    public void clearConversation(String conversationId) {
        memoryRepository.deleteByConversationId(conversationId);
    }

    private MessageDto toDto(Message message) {
        String role = message.getMessageType().getValue();
        return new MessageDto(role, message.getText(), Instant.now());
    }
}
