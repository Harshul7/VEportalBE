package org.example.veportal.dto.response;

public record StudentResponse(
        String id,
        String name,
        String programme,
        String batch,
        String status,
        String email,
        String createdAt,
        Double attendancePercent
) {
}
