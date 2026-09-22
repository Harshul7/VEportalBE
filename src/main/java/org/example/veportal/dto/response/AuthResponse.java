package org.example.veportal.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record AuthResponse(
        @JsonIgnore
        String token,
        UserResponse user
) {
}
