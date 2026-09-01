package org.example.veportal.dto.response;

public record BootstrapResponse(
        UserResponse user,
        CourseInfoResponse course,
        java.util.List<StudentResponse> students,
        java.util.List<SessionResponse> sessions,
        java.util.List<AttendanceRecordResponse> attendance,
        java.util.List<ParticipationRecordResponse> participation,
        java.util.List<MaterialResponse> materials,
        java.util.List<TeachingLogResponse> teachingLogs,
        java.util.List<NotificationResponse> notifications,
        java.util.List<ActivityItemResponse> activity,
        SummaryResponse summary
) {
}
