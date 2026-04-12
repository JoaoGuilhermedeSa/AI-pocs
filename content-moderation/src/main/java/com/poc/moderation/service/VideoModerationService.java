package com.poc.moderation.service;

import com.poc.moderation.dto.ModerationResponse;
import com.poc.moderation.dto.VideoModerationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class VideoModerationService {

    private static final Logger log = LoggerFactory.getLogger(VideoModerationService.class);

    private final ImageModerationService imageModerationService;
    private final TextModerationService textModerationService;

    @Value("${moderation.whisper.script}")
    private String whisperScript;

    @Value("${moderation.whisper.model}")
    private String whisperModelSize;

    @Value("${moderation.whisper.device}")
    private String whisperDevice;

    @Value("${moderation.ffmpeg.path}")
    private String ffmpegPath;

    @Value("${moderation.ffmpeg.max-frames}")
    private int maxFrames;

    @Value("${moderation.ffmpeg.fps}")
    private double fps;

    public VideoModerationService(ImageModerationService imageModerationService,
                                  TextModerationService textModerationService) {
        this.imageModerationService = imageModerationService;
        this.textModerationService = textModerationService;
    }

    public Mono<VideoModerationResponse> moderate(byte[] videoBytes, String originalFilename) {
        return Mono.fromCallable(() -> {
            Path tempDir = Files.createTempDirectory("moderation-video-");
            Path videoFile = tempDir.resolve("input" + getExtension(originalFilename));
            Files.write(videoFile, videoBytes);
            return new Object[]{tempDir, videoFile};
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(arr -> {
            Path tempDir = (Path) arr[0];
            Path videoFile = (Path) arr[1];

            Mono<List<byte[]>> framesMono = extractFrames(videoFile, tempDir);
            Mono<String> transcriptMono = extractAudioTranscript(videoFile, tempDir);

            return Mono.zip(framesMono, transcriptMono)
                    .flatMap(tuple -> {
                        List<byte[]> frames = tuple.getT1();
                        String transcript = tuple.getT2();

                        // Analyze all frames in parallel
                        Mono<ModerationResponse> visualMono = Flux.fromIterable(frames)
                                .flatMapSequential(frame -> imageModerationService.moderate(frame, "image/jpeg"))
                                .collectList()
                                .map(this::aggregateVisual);

                        // Analyze audio transcript
                        Mono<ModerationResponse> audioMono = transcript.isBlank()
                                ? Mono.just(safePlaceholder("No audio track detected"))
                                : textModerationService.moderate(transcript);

                        return Mono.zip(visualMono, audioMono)
                                .map(r -> aggregate(r.getT1(), r.getT2(), frames.size()));
                    })
                    .doFinally(sig -> cleanup(tempDir));
        });
    }

    // --- FFmpeg: extract JPEG frames ---

    private Mono<List<byte[]>> extractFrames(Path videoFile, Path tempDir) {
        return Mono.fromCallable(() -> {
            Path framesDir = tempDir.resolve("frames");
            Files.createDirectories(framesDir);

            List<String> cmd = List.of(
                    ffmpegPath,
                    "-i", videoFile.toString(),
                    "-vf", "fps=" + fps,
                    "-vframes", String.valueOf(maxFrames),
                    "-f", "image2",
                    framesDir.resolve("frame%04d.jpg").toString()
            );

            log.info("Extracting frames: {}", String.join(" ", cmd));
            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();

            String ffmpegOutput = new String(process.getInputStream().readAllBytes());
            int exit = process.waitFor();
            log.debug("FFmpeg frames exit={} output={}", exit, ffmpegOutput);

            List<byte[]> frames = new ArrayList<>();
            try (Stream<Path> files = Files.list(framesDir)) {
                files.sorted(Comparator.comparing(Path::toString))
                        .limit(maxFrames)
                        .forEach(f -> {
                            try { frames.add(Files.readAllBytes(f)); }
                            catch (IOException e) { log.warn("Could not read frame {}", f); }
                        });
            }
            log.info("Extracted {} frames from video", frames.size());
            return frames;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // --- FFmpeg: extract audio → WAV → transcribe via Ollama whisper (if available) ---

    private Mono<String> extractAudioTranscript(Path videoFile, Path tempDir) {
        return Mono.fromCallable(() -> {
            Path audioFile = tempDir.resolve("audio.wav");

            List<String> cmd = List.of(
                    ffmpegPath,
                    "-i", videoFile.toString(),
                    "-vn",                    // no video
                    "-ar", "16000",           // 16kHz sample rate (Whisper requirement)
                    "-ac", "1",               // mono
                    "-f", "wav",
                    audioFile.toString()
            );

            log.info("Extracting audio: {}", String.join(" ", cmd));
            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            String ffmpegOut = new String(process.getInputStream().readAllBytes());
            int exit = process.waitFor();
            long audioSize = Files.exists(audioFile) ? Files.size(audioFile) : 0;
            log.info("FFmpeg audio exit={} size={} bytes output={}", exit, audioSize, ffmpegOut.trim());

            if (exit != 0 || audioSize < 100) {
                log.warn("Audio extraction failed or no audio track. exit={} size={}", exit, audioSize);
                return "";
            }

            log.info("Audio extracted ({} bytes), transcribing with faster-whisper model={}", audioSize, whisperModelSize);
            return transcribeWithWhisper(audioFile);
        }).subscribeOn(Schedulers.boundedElastic())
          .onErrorResume(e -> {
              log.error("Audio pipeline error: {}", e.getMessage());
              return Mono.just("");
          });
    }

    private String transcribeWithWhisper(Path audioFile) {
        try {
            List<String> cmd = List.of(
                    "python", whisperScript,
                    audioFile.toString(),
                    whisperModelSize,
                    whisperDevice
            );
            log.info("Transcribing audio: {}", String.join(" ", cmd));

            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(false)
                    .start();

            String transcript = new String(process.getInputStream().readAllBytes()).trim();
            String errors     = new String(process.getErrorStream().readAllBytes()).trim();
            int exit = process.waitFor();

            log.info("faster-whisper exit={} transcript={}", exit, transcript);
            if (!errors.isBlank()) log.debug("faster-whisper stderr: {}", errors);

            return exit == 0 ? transcript : "";
        } catch (Exception e) {
            log.warn("faster-whisper transcription failed: {}", e.getMessage());
            return "";
        }
    }

    // --- Aggregation ---

    private ModerationResponse aggregateVisual(List<ModerationResponse> frameResults) {
        if (frameResults.isEmpty()) return safePlaceholder("No frames extracted");

        boolean anyUnsafe = frameResults.stream().anyMatch(r -> "UNSAFE".equals(r.verdict()));
        List<String> allCategories = frameResults.stream()
                .flatMap(r -> r.categories().stream())
                .filter(c -> !"NONE".equals(c))
                .distinct()
                .toList();
        double avgConfidence = frameResults.stream().mapToDouble(ModerationResponse::confidence).average().orElse(0.0);
        String severity = frameResults.stream()
                .map(ModerationResponse::severity)
                .max(Comparator.comparingInt(VideoModerationService::severityRank))
                .orElse("NONE");

        return new ModerationResponse(
                anyUnsafe ? "UNSAFE" : "SAFE",
                allCategories.isEmpty() ? List.of("NONE") : allCategories,
                severity,
                avgConfidence,
                anyUnsafe ? "Harmful visual content detected across video frames." : "No harmful visual content detected."
        );
    }

    private VideoModerationResponse aggregate(ModerationResponse visual, ModerationResponse audio, int frameCount) {
        boolean unsafe = "UNSAFE".equals(visual.verdict()) || "UNSAFE".equals(audio.verdict());
        List<String> categories = new ArrayList<>(visual.categories());
        audio.categories().forEach(c -> { if (!categories.contains(c)) categories.add(c); });
        categories.removeIf("NONE"::equals);
        if (categories.isEmpty()) categories.add("NONE");

        String severity = severityRank(visual.severity()) >= severityRank(audio.severity())
                ? visual.severity() : audio.severity();
        double confidence = Math.max(visual.confidence(), audio.confidence());

        String explanation = unsafe
                ? "Harmful content detected: " + (visual.verdict().equals("UNSAFE") ? "visual" : "") +
                  (audio.verdict().equals("UNSAFE") ? " audio" : "") + "."
                : "No harmful content detected in video frames or audio.";

        return new VideoModerationResponse(
                unsafe ? "UNSAFE" : "SAFE",
                categories,
                severity,
                confidence,
                explanation,
                visual,
                audio,
                frameCount
        );
    }

    private static ModerationResponse safePlaceholder(String reason) {
        return new ModerationResponse("SAFE", List.of("NONE"), "NONE", 1.0, reason);
    }

    private static int severityRank(String severity) {
        return switch (severity) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private static String getExtension(String filename) {
        if (filename == null) return ".mp4";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".mp4";
    }

    private void cleanup(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) {}
            });
        } catch (IOException e) {
            log.warn("Could not clean up temp dir {}", dir);
        }
    }
}
