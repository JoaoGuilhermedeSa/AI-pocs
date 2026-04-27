package com.poc.vision.controller;

import com.poc.vision.model.AnalyzeRequest;
import com.poc.vision.model.GenerateRequest;
import com.poc.vision.model.StreamEvent;
import com.poc.vision.service.ComfyUIService;
import com.poc.vision.service.ImageStorageService;
import com.poc.vision.service.OllamaVisionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping("/api/vision")
public class VisionController {

    private final ImageStorageService storage;
    private final OllamaVisionService ollamaVision;
    private final ComfyUIService comfyUI;
    private final ObjectMapper mapper;
    private final WebClient comfyClient;

    public VisionController(ImageStorageService storage, OllamaVisionService ollamaVision,
                            ComfyUIService comfyUI, ObjectMapper mapper) {
        this.storage = storage;
        this.ollamaVision = ollamaVision;
        this.comfyUI = comfyUI;
        this.mapper = mapper;
        this.comfyClient = WebClient.create("http://127.0.0.1:8188");
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Map<String, String>> upload(@RequestPart("file") FilePart file) {
        return storage.store(file).map(id -> Map.of("imageId", id));
    }

    @GetMapping("/preview/{imageId}")
    public Mono<Resource> preview(@PathVariable String imageId) {
        return Mono.just(new FileSystemResource(storage.resolve(imageId)));
    }

    @PostMapping(value = "/analyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> analyze(@RequestBody AnalyzeRequest req) {
        return Mono.fromCallable(() -> storage.toBase64(req.imageId()))
            .flatMapMany(base64 -> ollamaVision.analyze(base64, req.prompt()))
            .map(this::toSse)
            .onErrorResume(e -> Flux.just(errorSse(e.getMessage())));
    }

    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generate(@RequestBody GenerateRequest req) {
        return comfyUI.generate(req)
            .map(this::toSse)
            .onErrorResume(e -> Flux.just(errorSse(e.getMessage())));
    }

    // Proxy ComfyUI output images — streamed to avoid buffer size limits on large images
    @GetMapping("/output/{filename}")
    public Flux<DataBuffer> output(@PathVariable String filename, ServerHttpResponse response) {
        return comfyClient.get()
            .uri("/view?filename={f}&type=output", filename)
            .exchangeToFlux(r -> {
                response.getHeaders().setContentType(
                    r.headers().contentType().orElse(MediaType.IMAGE_PNG));
                return r.bodyToFlux(DataBuffer.class);
            });
    }

    private ServerSentEvent<String> toSse(StreamEvent event) {
        try {
            return ServerSentEvent.<String>builder()
                .event(event.type())
                .data(mapper.writeValueAsString(event))
                .build();
        } catch (Exception e) {
            return errorSse(e.getMessage());
        }
    }

    private ServerSentEvent<String> errorSse(String message) {
        return ServerSentEvent.<String>builder()
            .event("error")
            .data("{\"type\":\"error\",\"content\":" + "\"" + message + "\"}")
            .build();
    }
}
