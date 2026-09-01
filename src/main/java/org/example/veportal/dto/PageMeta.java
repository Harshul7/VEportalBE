package org.example.veportal.dto;

public record PageMeta(
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
