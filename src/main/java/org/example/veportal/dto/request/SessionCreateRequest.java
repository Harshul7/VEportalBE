package org.example.veportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SessionCreateRequest(
        Long courseId,

        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @NotBlank(message = "Topic is required")
        @Size(max = 255, message = "Topic must be at most 255 characters")
        String topic,

        @NotBlank(message = "Date is required")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Date must be in YYYY-MM-DD format")
        String date,

        @NotBlank(message = "Start time is required")
        @Pattern(regexp = "\\d{2}:\\d{2}", message = "Start time must be in HH:mm format")
        String startTime,

        @NotBlank(message = "End time is required")
        @Pattern(regexp = "\\d{2}:\\d{2}", message = "End time must be in HH:mm format")
        String endTime,

        @Size(max = 60, message = "Room must be at most 60 characters")
        String room,

        @Size(max = 30, message = "Mode must be at most 30 characters")
        String mode,

        @Size(max = 40, message = "Session type must be at most 40 characters")
        String sessionType,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @NotBlank(message = "Status is required")
        @Pattern(regexp = "Upcoming|Scheduled|Ongoing|Completed|Closed|Draft", message = "Invalid session status")
        String status
) {
}
