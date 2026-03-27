package com.poc.rag.dto;

import java.time.Instant;
import java.util.List;

public record SourceDocument(
        String id,
        String title,
        int chunkCount,
        Instant ingestedAt,
        List<String> chunkIds
) {}
