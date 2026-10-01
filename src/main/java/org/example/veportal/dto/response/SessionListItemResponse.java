package org.example.veportal.dto.response;

public record SessionListItemResponse(
        String id,
        int number,
        Long courseId,
        String courseName,
        Long chapterId,
        String chapterTitle,
        int chapterNumber,
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
