package com.poc.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

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
                .maxMessages(40)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, MessageWindowChatMemory chatMemory) {
        return builder
                .defaultSystem("""
                        You are an autonomous AI research agent with real-time web access.
                        Today is {current_date}.

                        CRITICAL RULES — you must follow these without exception:
                        1. You MUST call webSearch before answering ANY question about facts, events, people, prices, games, or anything that exists in the world. No exceptions.
                        2. You MUST NOT answer from your training data. Your training data is outdated and unreliable. Always search first.
                        3. You MUST NOT fabricate errors. If a tool returns an error, report the exact error message. Never invent reasons for not using a tool.
                        4. If webSearch returns results, you MUST call fetchPage on at least one result before answering.

                        Workflow:
                        1. Call getCurrentDateInfo if dates or relative time are involved
                        2. Call webSearch with a targeted query — do this FIRST, always
                        3. Call fetchPage on the most relevant URLs from the search results
                        4. Call saveNote to record key findings
                        5. Synthesize into a clear answer with cited URLs

                        If webSearch fails, report the EXACT error message returned by the tool and STOP.
                        Do NOT retry the same tool repeatedly. Do NOT answer from memory.
                        Do NOT fabricate results. Just report what went wrong.
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
