package com.poc.gamediscussion.config;

import com.poc.gamediscussion.participant.AIParticipant;
import com.poc.gamediscussion.participant.CliParticipant;
import com.poc.gamediscussion.participant.OllamaParticipant;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(AppConfig.DiscussionProperties.class)
public class AppConfig {

    @Bean
    public List<AIParticipant> aiParticipants(DiscussionProperties props) {
        List<AIParticipant> participants = new ArrayList<>();

        if (props.ollama().enabled()) {
            participants.add(new OllamaParticipant(
                "ollama", props.ollama().displayName(),
                props.ollama().model(), props.systemPrompt(),
                props.ollama().url()
            ));
        }
        if (props.claudeCli().enabled()) {
            participants.add(new CliParticipant(
                "claude", props.claudeCli().displayName(),
                props.claudeCli().command(), props.claudeCli().args(),
                props.systemPrompt()
            ));
        }
        if (props.geminiCli().enabled()) {
            participants.add(new CliParticipant(
                "gemini", props.geminiCli().displayName(),
                props.geminiCli().command(), props.geminiCli().args(),
                props.systemPrompt()
            ));
        }

        return participants;
    }

    @ConfigurationProperties("discussion")
    public record DiscussionProperties(
        String systemPrompt,
        OllamaConfig ollama,
        CliConfig claudeCli,
        CliConfig geminiCli
    ) {
        public record OllamaConfig(boolean enabled, String displayName, String model, String url) {}
        public record CliConfig(boolean enabled, String displayName, String command, List<String> args) {}
    }
}
