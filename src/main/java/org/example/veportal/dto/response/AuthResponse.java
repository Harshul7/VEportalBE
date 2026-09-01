package org.example.veportal.dto.response;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
