package org.example.veportal.dto.response;

public record MaterialResponse(
        String id,
        String title,
        String type,
        String sessionId,
        String uploadedBy,
        String uploadedAt,
        String size,
        String description
) {
}
