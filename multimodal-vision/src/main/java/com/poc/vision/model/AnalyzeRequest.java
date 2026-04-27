package com.poc.vision.model;

public record AnalyzeRequest(
    String imageId,
    String prompt
) {}
