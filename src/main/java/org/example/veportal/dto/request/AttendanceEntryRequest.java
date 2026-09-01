package org.example.veportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AttendanceEntryRequest(
        @NotBlank(message = "Student ID is required for each entry")
        String studentId,

        @NotBlank(message = "Attendance status is required for each entry")
        @Pattern(regexp = "Present|Absent", message = "Status must be Present or Absent")
        @Size(max = 10, message = "Invalid attendance status")
        String status
) {
}
