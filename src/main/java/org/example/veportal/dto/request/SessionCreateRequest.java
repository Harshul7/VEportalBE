package org.example.veportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SessionCreateRequest(
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

        @NotBlank(message = "Status is required")
        @Pattern(regexp = "Upcoming|Completed|Draft", message = "Status must be Upcoming, Completed or Draft")
        String status
) {
}
