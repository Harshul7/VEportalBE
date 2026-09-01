package org.example.veportal.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AttendanceSaveRequest(
        @Valid
        @NotEmpty(message = "At least one attendance entry is required")
        List<AttendanceEntryRequest> entries
) {
}
