package com.poc.assistant.service;

import com.poc.assistant.tools.CalendarTools;
import com.poc.assistant.tools.DateTools;
import com.poc.assistant.tools.EmailTools;
import com.poc.assistant.tools.TaskTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Service
public class AssistantService {

    private final ChatClient chatClient;
    private final CalendarTools calendarTools;
    private final EmailTools emailTools;
    private final TaskTools taskTools;
    private final DateTools dateTools;

    public AssistantService(ChatClient chatClient, CalendarTools calendarTools,
                            EmailTools emailTools, TaskTools taskTools, DateTools dateTools) {
        this.chatClient = chatClient;
        this.calendarTools = calendarTools;
        this.emailTools = emailTools;
        this.taskTools = taskTools;
        this.dateTools = dateTools;
    }

    public Flux<String> chat(String message, String conversationId) {
        return chatClient.prompt()
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .user(message)
                .tools(calendarTools, emailTools, taskTools, dateTools)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
