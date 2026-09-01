package org.example.veportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TeachingLogSaveRequest(
        @NotBlank(message = "Content of the teaching log is required")
        String content,

        @Size(max = 15, message = "At most 15 concepts are allowed")
        List<@Size(max = 120, message = "Each concept must be at most 120 characters") String> concepts,

        String remarks
) {
}
