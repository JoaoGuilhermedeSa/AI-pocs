package com.poc.rag.controller;

import com.poc.rag.dto.IngestTextRequest;
import com.poc.rag.dto.QueryRequest;
import com.poc.rag.dto.SourceDocument;
import com.poc.rag.service.IngestionService;
import com.poc.rag.service.RagService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * POST /api/rag/ingest/text          — ingest plain text {title, content}
 * POST /api/rag/ingest/file          — ingest uploaded file (.txt / .md / .pdf)
 * GET  /api/rag/documents            — list all source documents
 * DELETE /api/rag/documents/{id}     — remove a source document and its chunks
 * POST /api/rag/query/stream         — SSE streaming RAG answer
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final IngestionService ingestionService;
    private final RagService ragService;

    public RagController(IngestionService ingestionService, RagService ragService) {
        this.ingestionService = ingestionService;
        this.ragService = ragService;
    }

    @PostMapping("/ingest/text")
    public Mono<SourceDocument> ingestText(@RequestBody IngestTextRequest request) {
        return ingestionService.ingestText(request.title(), request.content());
    }

    @PostMapping(value = "/ingest/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<SourceDocument> ingestFile(@RequestPart("file") FilePart filePart,
                                           @RequestPart(value = "title", required = false) String title) {
        String docTitle = (title != null && !title.isBlank()) ? title : filePart.filename();
        boolean isPdf = filePart.filename().toLowerCase().endsWith(".pdf");

        return filePart.content()
                .reduce(new byte[0], (acc, buf) -> {
                    byte[] chunk = new byte[buf.readableByteCount()];
                    buf.read(chunk);
                    byte[] merged = new byte[acc.length + chunk.length];
                    System.arraycopy(acc, 0, merged, 0, acc.length);
                    System.arraycopy(chunk, 0, merged, acc.length, chunk.length);
                    return merged;
                })
                .flatMap(bytes -> isPdf
                        ? ingestionService.ingestPdf(docTitle, bytes)
                        : ingestionService.ingestTextFile(docTitle, bytes));
    }

    @GetMapping("/documents")
    public Flux<SourceDocument> listDocuments() {
        return Flux.fromIterable(ingestionService.listDocuments());
    }

    @DeleteMapping("/documents/{sourceId}")
    public Mono<Void> deleteDocument(@PathVariable String sourceId) {
        ingestionService.delete(sourceId);
        return Mono.empty();
    }

    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> queryStream(@RequestBody QueryRequest request) {
        return ragService.query(request.question())
                .map(token -> ServerSentEvent.<String>builder()
                        .event("token")
                        .data(token)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ))
                .onErrorResume(ex -> Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("error")
                                .data(ex.getMessage() != null ? ex.getMessage() : "Unknown error")
                                .build()
                ));
    }
}
