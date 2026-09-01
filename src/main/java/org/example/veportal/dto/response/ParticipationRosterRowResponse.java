package org.example.veportal.dto.response;

public record ParticipationRosterRowResponse(
        String studentId,
        String name,
        String programme,
        String batch,
        String level,
        String notes
) {
}
