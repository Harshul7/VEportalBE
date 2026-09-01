package org.example.veportal.controller;

import jakarta.validation.Valid;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.request.LoginRequest;
import org.example.veportal.dto.response.AuthResponse;
import org.example.veportal.dto.response.UserResponse;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AuthController(AuthService authService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.authService = authService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Signed in successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        UserResponse response = authService.currentUserProfile(authenticatedUserProvider.currentUser().getEmail());
        return ResponseEntity.ok(ApiResponse.success(response, "Current user retrieved"));
    }
}
