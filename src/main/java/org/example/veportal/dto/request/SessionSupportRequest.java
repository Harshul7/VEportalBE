package org.example.veportal.dto.request;

import jakarta.validation.constraints.NotNull;

public record SessionSupportRequest(@NotNull Long teachingAssistantId) {}
