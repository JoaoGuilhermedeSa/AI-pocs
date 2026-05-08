package com.poc.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "agent")
public record AgentProperties(
        Searxng searxng,
        Safety safety,
        Web web
) {
    public record Searxng(String url, int maxResults) {}

    public record Safety(int maxIterations, int maxFetchPerMinute, List<String> urlBlocklist) {}

    public record Web(int maxContentChars, int timeoutSeconds) {}
}
