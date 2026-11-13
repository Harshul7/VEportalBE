package org.example.veportal.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskSearchResponse(
        Long id,
        Long sessionId,
        String title,
        String taskType,
        String description,
        LocalDate dueDate,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String createdBy,
        LocalDateTime createdAt,
        String courseName,
        Integer sessionNumber,
        String sessionTopic,
        LocalDate sessionDate
) {}
