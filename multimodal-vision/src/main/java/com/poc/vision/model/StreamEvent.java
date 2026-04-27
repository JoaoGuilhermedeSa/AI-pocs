package com.poc.vision.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StreamEvent(
    String type,     // token | done | error | progress | image
    String content
) {}
