package org.example.veportal.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminTaskResponse(
        Long id,
        String title,
        String taskType,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        LocalDateTime createdAt,
        String createdBy,
        Long sessionId,
        Integer sessionNumber,
        String sessionTopic,
        LocalDate sessionDate,
        String groupName,
        Long mentorId,
        String mentorName,
        String mentorEmail,
        String courseCode,
        String courseName,
        String academicYearName
) {}
