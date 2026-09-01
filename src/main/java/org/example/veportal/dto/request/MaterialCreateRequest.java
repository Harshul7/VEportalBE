package org.example.veportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaterialCreateRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @NotBlank(message = "Material type is required")
        @Size(max = 30, message = "Unknown material type")
        String type,

        String sessionId,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @Size(max = 255, message = "File name must be at most 255 characters")
        String fileName,

        @Size(max = 20, message = "Size label must be at most 20 characters")
        String size
) {
}
