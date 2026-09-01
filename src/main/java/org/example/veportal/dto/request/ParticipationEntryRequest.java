package org.example.veportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParticipationEntryRequest(
        @NotBlank(message = "Student ID is required for each entry")
        String studentId,

        @NotBlank(message = "Participation level is required for each entry")
        @Size(max = 15, message = "Invalid participation level")
        String level,

        @Size(max = 500, message = "Notes must be at most 500 characters")
        String notes
) {
}
