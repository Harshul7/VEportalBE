package org.example.veportal.controller;

import jakarta.validation.Valid;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.request.ChangePasswordRequest;
import org.example.veportal.dto.request.ForgotPasswordRequest;
import org.example.veportal.dto.request.LoginRequest;
import org.example.veportal.dto.request.ResetPasswordRequest;
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

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(null, "If an account exists, a reset link was sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.password());
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authenticatedUserProvider.currentUser().getEmail(),
                request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }
}
