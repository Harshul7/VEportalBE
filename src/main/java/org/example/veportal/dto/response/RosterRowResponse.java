package org.example.veportal.dto.response;

public record RosterRowResponse(
        String studentId,
        String name,
        String programme,
        String batch,
        String status
) {
}
