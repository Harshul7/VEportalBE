package org.example.veportal.dto;

import java.time.Instant;
import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        PageMeta pagination,
        Map<String, String> errors,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> success(T data, String message, PageMeta pagination) {
        return new ApiResponse<>(true, message, data, pagination, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message, Map<String, String> errors) {
        return new ApiResponse<>(false, message, null, null, errors, Instant.now());
    }
}
