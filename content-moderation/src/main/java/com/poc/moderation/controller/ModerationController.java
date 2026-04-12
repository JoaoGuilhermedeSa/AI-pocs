package com.poc.moderation.controller;

import com.poc.moderation.dto.ModerationResponse;
import com.poc.moderation.dto.TextRequest;
import com.poc.moderation.dto.VideoModerationResponse;
import com.poc.moderation.service.ImageModerationService;
import com.poc.moderation.service.TextModerationService;
import com.poc.moderation.service.VideoModerationService;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * POST /api/moderation/text   — body: { "text": "..." }
 * POST /api/moderation/image  — multipart: file (image/jpeg, image/png, image/webp)
 * POST /api/moderation/video  — multipart: file (video/mp4, video/webm, etc.)
 */
@RestController
@RequestMapping("/api/moderation")
public class ModerationController {

    private final TextModerationService textService;
    private final ImageModerationService imageService;
    private final VideoModerationService videoService;

    public ModerationController(TextModerationService textService,
                                ImageModerationService imageService,
                                VideoModerationService videoService) {
        this.textService = textService;
        this.imageService = imageService;
        this.videoService = videoService;
    }

    @PostMapping("/text")
    public Mono<ModerationResponse> moderateText(@RequestBody TextRequest request) {
        return textService.moderate(request.text());
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ModerationResponse> moderateImage(@RequestPart("file") FilePart file) {
        String mimeType = Objects.requireNonNull(file.headers().getContentType(), "Missing Content-Type").toString();
        return DataBufferUtils.join(file.content())
                .map(buf -> {
                    byte[] bytes = new byte[buf.readableByteCount()];
                    buf.read(bytes);
                    DataBufferUtils.release(buf);
                    return bytes;
                })
                .flatMap(bytes -> imageService.moderate(bytes, mimeType));
    }

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<VideoModerationResponse> moderateVideo(@RequestPart("file") FilePart file) {
        return DataBufferUtils.join(file.content())
                .map(buf -> {
                    byte[] bytes = new byte[buf.readableByteCount()];
                    buf.read(bytes);
                    DataBufferUtils.release(buf);
                    return bytes;
                })
                .flatMap(bytes -> videoService.moderate(bytes, file.filename()));
    }
}
