package com.poc.gamediscussion.participant;

import com.poc.gamediscussion.model.ChatMessage;
import com.poc.gamediscussion.model.StreamEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CliParticipant implements AIParticipant {

    private final String id;
    private final String displayName;
    private final String command;
    private final List<String> extraArgs;
    private final String systemPrompt;

    public CliParticipant(String id, String displayName, String command, List<String> extraArgs, String systemPrompt) {
        this.id = id;
        this.displayName = displayName;
        this.command = command;
        this.extraArgs = extraArgs;
        this.systemPrompt = systemPrompt;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return displayName; }

    @Override
    public Flux<StreamEvent> chat(List<ChatMessage> history, String newMessage) {
        return Flux.<StreamEvent>create(sink ->
            Thread.ofVirtual().start(() -> runProcess(sink, history, newMessage))
        ).onErrorResume(e -> Flux.just(
            new StreamEvent(id, displayName, "error", e.getMessage()),
            new StreamEvent(id, displayName, "done", "")
        ));
    }

    private void runProcess(FluxSink<StreamEvent> sink, List<ChatMessage> history, String newMessage) {
        try {
            String prompt = buildPrompt(history, newMessage);

            List<String> cmd = new ArrayList<>();
            cmd.add(command);
            cmd.addAll(extraArgs);
            cmd.add(prompt);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.getOutputStream().close(); // signal EOF on stdin immediately

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                char[] buf = new char[256];
                int read;
                while ((read = reader.read(buf)) != -1) {
                    sink.next(new StreamEvent(id, displayName, "token", new String(buf, 0, read)));
                }
            }

            process.waitFor();
            sink.next(new StreamEvent(id, displayName, "done", ""));
            sink.complete();
        } catch (Exception e) {
            sink.next(new StreamEvent(id, displayName, "error", e.getClass().getSimpleName() + ": " + e.getMessage()));
            sink.next(new StreamEvent(id, displayName, "done", ""));
            sink.complete();
        }
    }

    private String buildPrompt(List<ChatMessage> history, String newMessage) {
        StringBuilder sb = new StringBuilder(systemPrompt).append("\n\n");

        if (!history.isEmpty()) {
            sb.append("Conversation so far:\n");
            for (ChatMessage msg : history) {
                if ("user".equals(msg.role())) {
                    sb.append("User: ").append(msg.content()).append("\n\n");
                } else {
                    sb.append(msg.participantName()).append(": ").append(msg.content()).append("\n\n");
                }
            }
        }

        sb.append("User: ").append(newMessage).append("\n\n");
        sb.append("Your response:");
        return sb.toString();
    }
}
