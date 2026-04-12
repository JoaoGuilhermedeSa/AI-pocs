package com.poc.moderation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.moderation.config.AiConfig;
import com.poc.moderation.dto.ModerationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Service
public class ImageModerationService {

    private static final Logger log = LoggerFactory.getLogger(ImageModerationService.class);
    private static final int MAX_DIM = 1024;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${moderation.models.vision}")
    private String visionModel;

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    public Mono<ModerationResponse> moderate(byte[] imageBytes, String mimeType) {
        return Mono.fromCallable(() -> {
            byte[] resized = resizeIfNeeded(imageBytes);
            String base64Image = Base64.getEncoder().encodeToString(resized);
            return callOllama(base64Image);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private ModerationResponse callOllama(String base64Image) throws Exception {
        String prompt = AiConfig.VISION_SYSTEM_PROMPT +
                "\n\nAnalyze this image for harmful content and respond with the required JSON.";

        // Build JSON with ObjectMapper to avoid any escaping/corruption in the base64 string
        var bodyNode = objectMapper.createObjectNode();
        bodyNode.put("model", visionModel);
        bodyNode.put("prompt", prompt);
        bodyNode.put("stream", false);
        bodyNode.putObject("options").put("temperature", 0.1);
        bodyNode.putArray("images").add(base64Image);
        String body = objectMapper.writeValueAsString(bodyNode);

        log.info("Sending image to Ollama: {} bytes base64, body size {} bytes", base64Image.length(), body.length());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaBaseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("Ollama vision status={} body={}", response.statusCode(), response.body());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama vision error: " + response.body());
        }

        String content = objectMapper.readTree(response.body())
                .path("response").asText("");

        log.debug("Vision raw content: {}", content);
        return parse(content);
    }

    private byte[] resizeIfNeeded(byte[] imageBytes) throws Exception {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null) return imageBytes;

        int w = original.getWidth();
        int h = original.getHeight();

        // Resize if needed
        BufferedImage source = original;
        if (w > MAX_DIM || h > MAX_DIM) {
            double scale = (double) MAX_DIM / Math.max(w, h);
            int newW = (int) (w * scale);
            int newH = (int) (h * scale);
            log.info("Resizing image {}x{} → {}x{}", w, h, newW, newH);
            BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D gr = resized.createGraphics();
            gr.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gr.drawImage(original, 0, 0, newW, newH, null);
            gr.dispose();
            source = resized;
        }

        // Always flatten to RGB (removes alpha channel) and encode as JPEG with explicit quality.
        // llava:13b crashes on alpha-channel PNG — JPEG is safest.
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        g.drawImage(source, 0, 0, null);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.85f);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(rgb, null, null), param);
        }
        writer.dispose();

        log.info("Image prepared for llava: {}x{}, {} bytes", rgb.getWidth(), rgb.getHeight(), out.size());
        return out.toByteArray();
    }

    ModerationResponse parse(String raw) throws Exception {
        String cleaned = raw.replaceAll("(?s)<think>.*?</think>", "").trim();
        cleaned = cleaned.replaceAll("(?s)```[a-z]*\\s*", "").replace("```", "").trim();
        return objectMapper.readValue(cleaned, ModerationResponse.class);
    }
}
