package org.example.veportal.dto.response;

public record UserResponse(
        String name,
        String email,
        String role,
        String facultyId,
        String department,
        String status,
        boolean mustChangePassword
) {
}