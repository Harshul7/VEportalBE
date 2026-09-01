package org.example.veportal.dto.response;

public record TeachingLogHistoryItemResponse(
        String id,
        String sessionId,
        int sessionNumber,
        String date,
        String topic,
        String updatedAt
) {
}
