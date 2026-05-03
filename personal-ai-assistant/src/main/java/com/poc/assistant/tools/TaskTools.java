package com.poc.assistant.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.poc.assistant.model.Task;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TaskTools {

    private static final String TASKS_FILE = "tasks.json";
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private List<Task> tasks = new ArrayList<>();

    @PostConstruct
    void load() {
        try {
            var file = new File(TASKS_FILE);
            if (file.exists()) {
                tasks = mapper.readValue(file, new TypeReference<List<Task>>() {});
            }
        } catch (Exception ignored) {}
    }

    private void save() {
        try { mapper.writeValue(new File(TASKS_FILE), tasks); } catch (Exception ignored) {}
    }

    @Tool(description = "Create a new task. dueDate must be YYYY-MM-DD or empty string for no due date.")
    public String createTask(String title, String description, String dueDate) {
        var id = UUID.randomUUID().toString().substring(0, 8);
        LocalDate due = (dueDate != null && !dueDate.isBlank()) ? LocalDate.parse(dueDate) : null;
        tasks.add(new Task(id, title, description, due, "PENDING"));
        save();
        return "Task created: [" + id + "] " + title;
    }

    @Tool(description = "List tasks. status must be PENDING, DONE, or ALL.")
    public String listTasks(String status) {
        var filtered = tasks.stream()
                .filter(t -> "ALL".equalsIgnoreCase(status) || t.status().equalsIgnoreCase(status))
                .toList();
        if (filtered.isEmpty()) return "No tasks found with status: " + status;
        return filtered.stream()
                .map(t -> "- [%s] %s | Due: %s | %s".formatted(
                        t.id(), t.title(),
                        t.dueDate() != null ? t.dueDate() : "—",
                        t.status()))
                .collect(Collectors.joining("\n", "Tasks (" + status + "):\n", ""));
    }

    @Tool(description = "Mark a task as DONE by its short ID (e.g. 'a1b2c3d4').")
    public String completeTask(String taskId) {
        boolean found = tasks.stream().anyMatch(t -> t.id().equals(taskId));
        if (!found) return "Task not found: " + taskId;
        tasks.replaceAll(t -> t.id().equals(taskId)
                ? new Task(t.id(), t.title(), t.description(), t.dueDate(), "DONE") : t);
        save();
        return "Task " + taskId + " marked as DONE.";
    }

    @Tool(description = "Delete a task permanently by its short ID.")
    public String deleteTask(String taskId) {
        boolean removed = tasks.removeIf(t -> t.id().equals(taskId));
        if (removed) save();
        return removed ? "Task " + taskId + " deleted." : "Task not found: " + taskId;
    }
}
