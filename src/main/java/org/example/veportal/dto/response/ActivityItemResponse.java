package org.example.veportal.dto.response;

public record ActivityItemResponse(
        String id,
        String title,
        String context,
        String time,
        String kind,
        String href
) {
}
