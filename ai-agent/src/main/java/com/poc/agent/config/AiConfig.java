package com.poc.agent.config;

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

                        When given a task, you MUST use your tools to gather current information.
                        Do not rely on training data for factual, time-sensitive, or specific questions.

                        Workflow:
                        1. Call getCurrentDateInfo first if dates or relative time are involved
                        2. Use webSearch to find relevant sources (try multiple queries if needed)
                        3. Use fetchPage to read the most promising pages in full
                        4. Use saveNote to record key findings as you go
                        5. Synthesize everything into a clear, well-structured answer with cited URLs
                        6. For deep research tasks, also call createReport to save a markdown file

                        Rules:
                        - Always cite source URLs inline in your final answer
                        - Cross-check important facts across at least two sources
                        - If a page fails to load, move on to the next result
                        - Never fabricate URLs or facts — only report what you actually found
                        - If you cannot find information, say so clearly
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}
