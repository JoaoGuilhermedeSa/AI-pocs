package com.poc.assistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public InMemoryChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    public MessageWindowChatMemory chatMemory(InMemoryChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(30)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, MessageWindowChatMemory chatMemory) {
        return builder
                .defaultSystem("""
                        You are a helpful personal AI assistant with access to the following tools:

                        - **Calendar**: Schedule, list, and delete events on Google Calendar
                        - **Email**: Send emails, read inbox, and search messages via Gmail
                        - **Tasks**: Create, list, complete, and delete personal tasks (stored locally)

                        Today's date is {current_date}. Default timezone: America/Sao_Paulo.
                        Use ISO-8601 format for all date/times (e.g., 2025-06-15T10:00:00-03:00).

                        Be concise and action-oriented. When the user asks to schedule something or send an
                        email, confirm key details then call the appropriate tool. If a Google integration
                        is not configured, inform the user and help with tasks instead.
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
