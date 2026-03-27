package com.poc.rag.service;

import com.poc.rag.dto.SourceDocument;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IngestionService {

    private final EmbeddingStore embeddingStore;
    private final TokenTextSplitter splitter;
    private final Map<String, SourceDocument> sourceDocs = new ConcurrentHashMap<>();

    public IngestionService(EmbeddingStore embeddingStore) {
        this.embeddingStore = embeddingStore;
        this.splitter = new TokenTextSplitter();
    }

    public Mono<SourceDocument> ingestText(String title, String content) {
        String sourceId = UUID.randomUUID().toString();
        return Mono.fromCallable(() -> splitter.apply(List.of(new Document(content))))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(chunks -> ingestChunks(sourceId, title, chunks));
    }

    public Mono<SourceDocument> ingestPdf(String title, byte[] pdfBytes) {
        String sourceId = UUID.randomUUID().toString();
        return Mono.fromCallable(() -> {
            ByteArrayResource resource = new ByteArrayResource(pdfBytes) {
                @Override public String getFilename() { return title + ".pdf"; }
            };
            List<Document> pages = new PagePdfDocumentReader(resource).read();
            return splitter.apply(pages);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(chunks -> ingestChunks(sourceId, title, chunks));
    }

    public Mono<SourceDocument> ingestTextFile(String title, byte[] fileBytes) {
        return ingestText(title, new String(fileBytes));
    }

    public boolean delete(String sourceId) {
        if (!sourceDocs.containsKey(sourceId)) return false;
        sourceDocs.remove(sourceId);
        embeddingStore.deleteBySourceId(sourceId);
        return true;
    }

    public Collection<SourceDocument> listDocuments() {
        return sourceDocs.values();
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private Mono<SourceDocument> ingestChunks(String sourceId, String title, List<Document> chunks) {
        return Flux.fromIterable(chunks)
                .concatMap(c -> embeddingStore.add(sourceId, title, c.getText()))
                .collectList()
                .map(chunkIds -> {
                    SourceDocument src = new SourceDocument(sourceId, title, chunkIds.size(), Instant.now(), chunkIds);
                    sourceDocs.put(sourceId, src);
                    return src;
                });
    }
}
