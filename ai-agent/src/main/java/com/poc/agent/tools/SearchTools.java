package com.poc.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.agent.config.AgentProperties;
import com.poc.agent.safety.AgentContext;
import com.poc.agent.safety.AgentTrace;
import com.poc.agent.safety.SafetyGuard;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.stream.Collectors;

@Component
public class SearchTools {

    private final WebClient webClient;
    private final AgentProperties props;
    private final AgentTrace trace;
    private final AgentContext agentContext;
    private final SafetyGuard safetyGuard;
    private final ObjectMapper objectMapper;

    public SearchTools(WebClient webClient, AgentProperties props, AgentTrace trace,
                       AgentContext agentContext, SafetyGuard safetyGuard, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.props = props;
        this.trace = trace;
        this.agentContext = agentContext;
        this.safetyGuard = safetyGuard;
        this.objectMapper = objectMapper;
    }

    @Tool(description = """
            Search the web using SearXNG. Returns titles, URLs, and snippets for the top results.
            Use specific, targeted queries for best results. Call multiple times with different queries
            to cover a topic from different angles.
            """)
    public String webSearch(String query) {
        safetyGuard.checkIterations();
        var convId = agentContext.getConversationId();
        trace.log(convId, "webSearch", "Searching: " + query);

        try {
            var uri = UriComponentsBuilder.fromUriString(props.searxng().url() + "/search")
                    .queryParam("q", query)
                    .queryParam("format", "json")
                    .queryParam("pageno", 1)
                    .build()
                    .toUri();

            var raw = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(status -> status.isError(), r -> r.bodyToMono(String.class)
                            .map(body -> new RuntimeException(
                                    "SearXNG returned HTTP " + r.statusCode().value() +
                                    ". Body: " + body.substring(0, Math.min(body.length(), 300)))))
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            var response = objectMapper.readTree(raw);

            if (response == null || !response.has("results")) {
                return "No results found for: " + query;
            }

            var results = response.get("results");
            var maxResults = props.searxng().maxResults();
            var sb = new StringBuilder("Search results for \"" + query + "\":\n\n");
            int count = 0;

            for (JsonNode r : results) {
                if (count++ >= maxResults) break;
                var title = r.path("title").asText("(no title)");
                var url = r.path("url").asText("");
                var content = r.path("content").asText("").strip();
                sb.append("[%d] %s\nURL: %s\nSnippet: %s\n\n".formatted(count, title, url, content));
            }

            return sb.toString().trim();
        } catch (Exception ex) {
            return "Search failed: " + ex.getMessage() +
                    "\nMake sure SearXNG is running at " + props.searxng().url();
        }
    }
}
