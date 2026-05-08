package com.poc.agent.tools;

import com.poc.agent.safety.AgentContext;
import com.poc.agent.safety.AgentTrace;
import com.poc.agent.safety.SafetyGuard;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ReportTools {

    private static final String REPORTS_DIR = "reports";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final AgentTrace trace;
    private final AgentContext agentContext;
    private final SafetyGuard safetyGuard;

    public ReportTools(AgentTrace trace, AgentContext agentContext, SafetyGuard safetyGuard) {
        this.trace = trace;
        this.agentContext = agentContext;
        this.safetyGuard = safetyGuard;
    }

    @Tool(description = """
            Save a structured research report as a markdown file in the reports/ directory.
            Use this at the end of complex research tasks to persist your findings.
            filename should be a short slug like 'ai_market_analysis'. Content should be full markdown.
            """)
    public String createReport(String filename, String title, String content) {
        safetyGuard.checkIterations();
        var convId = agentContext.getConversationId();
        trace.log(convId, "createReport", "Creating report: " + title);

        try {
            Files.createDirectories(Path.of(REPORTS_DIR));
            var timestamp = LocalDateTime.now().format(FMT);
            var safeName = filename.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            var path = Path.of(REPORTS_DIR, "%s_%s.md".formatted(timestamp, safeName));

            var markdown = "# %s\n\n*Generated: %s*\n\n%s".formatted(
                    title, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), content);

            Files.writeString(path, markdown);
            return "Report saved to: " + path.toAbsolutePath();
        } catch (IOException ex) {
            return "Failed to save report: " + ex.getMessage();
        }
    }
}
