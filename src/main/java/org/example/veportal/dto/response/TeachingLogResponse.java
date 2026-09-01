package org.example.veportal.dto.response;

import java.util.List;

public record TeachingLogResponse(
        String id,
        String sessionId,
        String content,
        List<String> concepts,
        String remarks,
        String updatedAt
) {
}
