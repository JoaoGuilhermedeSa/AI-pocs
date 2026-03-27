package com.poc.rag.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight in-memory vector store backed by cosine similarity.
 * All calls to EmbeddingModel are wrapped in boundedElastic to prevent
 * blocking on Reactor NIO threads.
 */
@Service
public class EmbeddingStore {

    public record Chunk(String id, String sourceId, String title, String text, float[] embedding) {}

    private final EmbeddingModel embeddingModel;
    private final List<Chunk> store = new CopyOnWriteArrayList<>();

    public EmbeddingStore(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Mono<String> add(String sourceId, String title, String text) {
        return Mono.fromCallable(() -> {
            float[] embedding = embeddingModel.embed(text);
            String id = UUID.randomUUID().toString();
            store.add(new Chunk(id, sourceId, title, text, embedding));
            return id;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<Chunk>> search(String query, int topK) {
        return Mono.fromCallable(() -> {
            float[] queryEmbedding = embeddingModel.embed(query);
            return store.stream()
                    .map(c -> Map.entry(c, cosineSimilarity(queryEmbedding, c.embedding())))
                    .sorted(Map.Entry.<Chunk, Double>comparingByValue().reversed())
                    .limit(topK)
                    .map(Map.Entry::getKey)
                    .toList();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public void deleteBySourceId(String sourceId) {
        store.removeIf(c -> c.sourceId().equals(sourceId));
    }

    public boolean isEmpty() {
        return store.isEmpty();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        return (normA == 0 || normB == 0) ? 0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
