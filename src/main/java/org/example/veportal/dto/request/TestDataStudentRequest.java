package org.example.veportal.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TestDataStudentRequest(
        @NotBlank(message = "Student code is required")
        @Size(max = 30, message = "Student code must be at most 30 characters")
        String studentCode,

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        @Email(message = "Email must be a valid address")
        @Size(max = 190, message = "Email must be at most 190 characters")
        String email,

        @NotBlank(message = "Programme is required")
        @Size(max = 80, message = "Programme must be at most 80 characters")
        String programme,

        @Size(max = 20, message = "Batch must be at most 20 characters")
        String batch,

        @Size(max = 20, message = "Gender must be at most 20 characters")
        String gender,

        String dateOfBirth,

        @NotBlank(message = "Status is required")
        @Pattern(regexp = "Active|Inactive", message = "Status must be Active or Inactive")
        String status,

        @Size(max = 20, message = "Mobile must be at most 20 characters")
        String mobile,

        @Size(max = 255, message = "Address must be at most 255 characters")
        String address,

        @Size(max = 80, message = "City must be at most 80 characters")
        String city,

        @Size(max = 80, message = "State must be at most 80 characters")
        String state,

        @Size(max = 10, message = "Pincode must be at most 10 characters")
        String pincode,

        @Size(max = 150, message = "Father name must be at most 150 characters")
        String fatherName,

        @Size(max = 150, message = "Mother name must be at most 150 characters")
        String motherName,

        String admissionDate,

        Boolean scholarship,

        @Size(max = 500, message = "Notes must be at most 500 characters")
        String notes,

        @Size(max = 20, message = "Emergency contact must be at most 20 characters")
        String emergencyContact,

        @Size(max = 10, message = "Blood group must be at most 10 characters")
        String bloodGroup
) {
}
