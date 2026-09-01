package org.example.veportal.dto.response;

public record NotificationResponse(
        String id,
        String title,
        String body,
        String time,
        Boolean read,
        String href
) {
}
