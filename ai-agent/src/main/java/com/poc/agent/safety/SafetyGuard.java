package com.poc.agent.safety;

import com.poc.agent.config.AgentProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Component
public class SafetyGuard {

    private static final Pattern PRIVATE_IP = Pattern.compile(
            "^(localhost|127\\.|10\\.|192\\.168\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.|0\\.0\\.0\\.0|::1).*");

    private final AgentProperties props;
    private final AgentContext agentContext;

    private final AtomicInteger fetchCount = new AtomicInteger(0);
    private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

    public SafetyGuard(AgentProperties props, AgentContext agentContext) {
        this.props = props;
        this.agentContext = agentContext;
    }

    public void checkUrl(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (Exception e) {
            throw new SecurityException("Invalid URL: " + url);
        }

        var scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new SecurityException("Only http/https URLs are allowed. Got: " + scheme);
        }

        var host = uri.getHost();
        if (host == null || PRIVATE_IP.matcher(host).matches()) {
            throw new SecurityException("Access to private/local addresses is blocked: " + host);
        }

        var blocklist = props.safety().urlBlocklist();
        if (blocklist != null && blocklist.stream().anyMatch(host::contains)) {
            throw new SecurityException("URL is blocked by policy: " + host);
        }
    }

    public void checkIterations() {
        int count = agentContext.incrementAndGetIterations();
        int max = props.safety().maxIterations();
        if (count > max) {
            throw new RuntimeException("Agent iteration limit reached (%d/%d). Stopping to prevent runaway execution.".formatted(count, max));
        }
    }

    public void checkFetchRate() {
        long now = System.currentTimeMillis();
        if (now - windowStart.get() > 60_000) {
            windowStart.set(now);
            fetchCount.set(0);
        }
        int count = fetchCount.incrementAndGet();
        if (count > props.safety().maxFetchPerMinute()) {
            throw new RuntimeException("Web fetch rate limit exceeded (%d/min). Please wait a moment.".formatted(props.safety().maxFetchPerMinute()));
        }
    }
}
