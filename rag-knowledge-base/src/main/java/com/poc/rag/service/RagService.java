package com.poc.rag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.stream.Collectors;

@Service
public class RagService {

    private static final int TOP_K = 5;

    private final ChatClient chatClient;
    private final EmbeddingStore embeddingStore;

    public RagService(ChatClient chatClient, EmbeddingStore embeddingStore) {
        this.chatClient = chatClient;
        this.embeddingStore = embeddingStore;
    }

    public Flux<String> query(String question) {
        if (embeddingStore.isEmpty()) {
            return Flux.just("The knowledge base is empty. Please add some documents first.");
        }

        return embeddingStore.search(question, TOP_K)
                .flatMapMany(relevant -> {
                    String context = relevant.stream()
                            .map(c -> "--- Source: " + c.title() + " ---\n" + c.text())
                            .collect(Collectors.joining("\n\n"));

                    String prompt = """
                            Context documents:
                            %s

                            Question: %s
                            """.formatted(context, question);

                    return chatClient.prompt()
                            .user(prompt)
                            .stream()
                            .content();
                });
    }
}
