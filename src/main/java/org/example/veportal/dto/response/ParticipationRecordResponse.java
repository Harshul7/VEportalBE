package org.example.veportal.dto.response;

public record ParticipationRecordResponse(
        String id,
        String sessionId,
        String studentId,
        String level,
        String notes
) {
}
