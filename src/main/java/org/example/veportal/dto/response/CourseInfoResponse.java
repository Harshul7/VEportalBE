package org.example.veportal.dto.response;

public record CourseInfoResponse(
        String name,
        String id,
        String term,
        String faculty,
        String status,
        String summary
) {
}
