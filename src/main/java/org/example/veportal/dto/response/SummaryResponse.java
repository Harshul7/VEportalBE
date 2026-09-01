package org.example.veportal.dto.response;

public record SummaryResponse(
        long activeStudents,
        long classesConducted,
        double attendancePercent,
        long materialsCount,
        long completedSessions,
        long savedLogs,
        long sessionsWithParticipation
) {
}
