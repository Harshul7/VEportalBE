package org.example.veportal.dto.response;

public record AdminActivityResponse(
        String id,
        String title,
        String context,
        String time,
        String kind,
        String href,
        String groupName,
        String actorName,
        String actorEmail
) {
}
