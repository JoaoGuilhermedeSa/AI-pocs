package com.poc.agent.model;

import java.time.Instant;

public record TraceEntry(String tool, String description, Instant timestamp) {}
