package com.poc.sentiment.dto;

import java.util.List;

public record BatchRequest(List<String> texts) {}
