package com.poc.vision.model;

public record GenerateRequest(
    String prompt,
    int width,
    int height,
    int steps,
    long seed
) {
    public GenerateRequest {
        if (width  <= 0) width  = 1024;
        if (height <= 0) height = 1024;
        if (steps  <= 0) steps  = 20;
        if (seed   == 0) seed   = System.currentTimeMillis();
    }
}
