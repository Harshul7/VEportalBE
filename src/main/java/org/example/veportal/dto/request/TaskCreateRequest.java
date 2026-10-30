package org.example.veportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record TaskCreateRequest(
        @NotBlank String title,
        @NotBlank String taskType,
        String description,
        LocalDate dueDate
) {}
