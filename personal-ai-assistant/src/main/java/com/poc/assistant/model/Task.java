package com.poc.assistant.model;

import java.time.LocalDate;

public record Task(String id, String title, String description, LocalDate dueDate, String status) {}
