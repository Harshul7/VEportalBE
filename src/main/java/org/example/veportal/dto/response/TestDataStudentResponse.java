package org.example.veportal.dto.response;

public record TestDataStudentResponse(
        Long id,
        String studentCode,
        String fullName,
        String email,
        String programme,
        String batch,
        String gender,
        String dateOfBirth,
        String status,
        String mobile,
        String address,
        String city,
        String state,
        String pincode,
        String fatherName,
        String motherName,
        String admissionDate,
        Boolean scholarship,
        String notes,
        String emergencyContact,
        String bloodGroup,
        String createdAt
) {
}
