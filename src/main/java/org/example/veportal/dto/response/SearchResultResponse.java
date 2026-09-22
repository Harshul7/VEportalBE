package org.example.veportal.dto.response;

public record SearchResultResponse(
        String type,
        String id,
        String title,
        String subtitle,
        String href
) {
}
