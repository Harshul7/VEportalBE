package org.example.veportal.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentCreateRequest(
        @NotBlank(message = "Student ID is required")
        @Size(max = 30, message = "Student ID must be at most 30 characters")
        String studentCode,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @NotBlank(message = "Programme is required")
        @Size(max = 80, message = "Programme must be at most 80 characters")
        String programme,

        @NotBlank(message = "Batch is required")
        @Size(max = 20, message = "Batch must be at most 20 characters")
        String batch,

        @NotBlank(message = "Status is required")
        @jakarta.validation.constraints.Pattern(regexp = "Active|Inactive", message = "Status must be Active or Inactive")
        String status,

        @Email(message = "Email must be a valid address")
        @Size(max = 190, message = "Email must be at most 190 characters")
        String email
) {
}
