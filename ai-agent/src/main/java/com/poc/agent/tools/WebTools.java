package com.poc.agent.tools;

import com.poc.agent.config.AgentProperties;
import com.poc.agent.safety.AgentContext;
import com.poc.agent.safety.AgentTrace;
import com.poc.agent.safety.SafetyGuard;
import org.jsoup.Jsoup;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class WebTools {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final AgentProperties props;
    private final AgentTrace trace;
    private final AgentContext agentContext;
    private final SafetyGuard safetyGuard;

    public WebTools(AgentProperties props, AgentTrace trace,
                    AgentContext agentContext, SafetyGuard safetyGuard) {
        this.props = props;
        this.trace = trace;
        this.agentContext = agentContext;
        this.safetyGuard = safetyGuard;
    }

    @Tool(description = """
            Fetch and read the full text content of a web page at the given URL.
            Strips navigation, ads, scripts, and other noise — returns clean readable text.
            Use this after webSearch to read promising pages in detail.
            """)
    public String fetchPage(String url) {
        safetyGuard.checkIterations();
        safetyGuard.checkUrl(url);
        safetyGuard.checkFetchRate();

        var convId = agentContext.getConversationId();
        trace.log(convId, "fetchPage", "Fetching: " + url);

        try {
            var doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(props.web().timeoutSeconds() * 1000)
                    .followRedirects(true)
                    .get();

            doc.select("script, style, nav, footer, header, aside, iframe, " +
                    "[class*=cookie], [class*=popup], [class*=banner], [class*=modal], " +
                    "[class*=subscribe], [id*=cookie], [id*=popup], [id*=banner]").remove();

            var title = doc.title();
            var text = doc.body().text();
            var maxChars = props.web().maxContentChars();

            if (text.length() > maxChars) {
                text = text.substring(0, maxChars) + "\n\n[Content truncated at " + maxChars + " characters]";
            }

            return "Page: %s\nURL: %s\n\n%s".formatted(title, url, text);
        } catch (SecurityException ex) {
            return "Blocked: " + ex.getMessage();
        } catch (Exception ex) {
            return "Failed to fetch %s: %s".formatted(url, ex.getMessage());
        }
    }
}
