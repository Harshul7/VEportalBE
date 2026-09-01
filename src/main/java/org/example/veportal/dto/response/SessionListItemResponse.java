package org.example.veportal.dto.response;

public record SessionListItemResponse(
        String id,
        int number,
        String topic,
        String date,
        String startTime,
        String endTime,
        String room,
        String facultyName,
        String status,
        Double attendancePercent,
        boolean hasMaterials,
        boolean teachingLogSaved
) {
}
