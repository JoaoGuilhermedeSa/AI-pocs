package com.poc.agent.service;

import com.poc.agent.safety.AgentContext;
import com.poc.agent.safety.AgentTrace;
import com.poc.agent.tools.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Service
public class AgentService {

    private final ChatClient chatClient;
    private final AgentContext agentContext;
    private final AgentTrace agentTrace;
    private final SearchTools searchTools;
    private final WebTools webTools;
    private final ScratchpadTools scratchpadTools;
    private final ReportTools reportTools;
    private final DateTools dateTools;

    public AgentService(ChatClient chatClient, AgentContext agentContext, AgentTrace agentTrace,
                        SearchTools searchTools, WebTools webTools, ScratchpadTools scratchpadTools,
                        ReportTools reportTools, DateTools dateTools) {
        this.chatClient = chatClient;
        this.agentContext = agentContext;
        this.agentTrace = agentTrace;
        this.searchTools = searchTools;
        this.webTools = webTools;
        this.scratchpadTools = scratchpadTools;
        this.reportTools = reportTools;
        this.dateTools = dateTools;
    }

    public Flux<String> chat(String message, String conversationId) {
        agentContext.start(conversationId);
        agentTrace.clear(conversationId);

        return chatClient.prompt()
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .user(message)
                .tools(searchTools, webTools, scratchpadTools, reportTools, dateTools)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .doFinally(signal -> agentContext.clear(conversationId));
    }
}
