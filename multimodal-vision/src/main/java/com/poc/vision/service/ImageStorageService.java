package com.poc.vision.service;

import com.poc.vision.config.VisionProperties;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.io.IOException;
import java.nio.file.*;
import java.util.Base64;
import java.util.UUID;

@Service
public class ImageStorageService {

    private final Path tempDir;

    public ImageStorageService(VisionProperties props) throws IOException {
        this.tempDir = Path.of(props.getStorage().getTempDir());
        Files.createDirectories(tempDir);
    }

    public Mono<String> store(FilePart file) {
        String id = UUID.randomUUID().toString();
        String ext = extractExtension(file.filename());
        Path dest = tempDir.resolve(id + ext);
        return file.transferTo(dest).thenReturn(id + ext);
    }

    public String toBase64(String imageId) throws IOException {
        Path file = tempDir.resolve(imageId);
        byte[] bytes = Files.readAllBytes(file);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public Path resolve(String imageId) {
        return tempDir.resolve(imageId);
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}
