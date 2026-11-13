package org.example.veportal.dto.response;

public record AdminActivityResponse(
        String id,
        String title,
        String context,
        String time,
        String kind,
        String href,
        String groupName,
        String courseName,
        String academicYearName,
        String actorName,
        String actorEmail
) {
}
