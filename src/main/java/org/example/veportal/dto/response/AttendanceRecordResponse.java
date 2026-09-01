package org.example.veportal.dto.response;

public record AttendanceRecordResponse(
        String id,
        String sessionId,
        String studentId,
        String status
) {
}
