package org.example.veportal.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskResponse(
        Long id,
        Long sessionId,
        String title,
        String taskType,
        String description,
        LocalDate dueDate,
        String status,
        String createdBy,
        LocalDateTime createdAt,
        List<TaskDocumentResponse> documents
) {
    public record TaskDocumentResponse(Long id, String fileName, String contentType,
                                       long sizeBytes, String uploadedBy, LocalDateTime uploadedAt) {}
}
