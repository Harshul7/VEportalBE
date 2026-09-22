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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import java.security.SecureRandom;
import java.util.Base64;
import org.example.veportal.config.AppProperties;
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
    private final AppProperties appProperties;

    public AuthController(AuthService authService, AuthenticatedUserProvider authenticatedUserProvider,
                          AppProperties appProperties) {
        this.authService = authService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.appProperties = appProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        ResponseCookie cookie = ResponseCookie.from(appProperties.jwt().cookieName(), response.token())
                .httpOnly(true).secure(appProperties.jwt().cookieSecure()).sameSite("Lax")
                .path("/").maxAge(appProperties.jwt().expirationMinutes() * 60).build();
        ResponseCookie csrf = ResponseCookie.from("ve_csrf", csrfToken())
                .secure(appProperties.jwt().cookieSecure()).sameSite("Lax").path("/")
                .maxAge(appProperties.jwt().expirationMinutes() * 60).build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.SET_COOKIE, csrf.toString())
                .body(ApiResponse.success(response, "Signed in successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        ResponseCookie cookie = ResponseCookie.from(appProperties.jwt().cookieName(), "")
                .httpOnly(true).secure(appProperties.jwt().cookieSecure()).sameSite("Lax")
                .path("/").maxAge(0).build();
        ResponseCookie csrf = ResponseCookie.from("ve_csrf", "")
                .secure(appProperties.jwt().cookieSecure()).sameSite("Lax").path("/").maxAge(0).build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.SET_COOKIE, csrf.toString())
                .body(ApiResponse.success(null, "Signed out successfully"));
    }

    private String csrfToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        UserResponse response = authService.currentUserProfile(authenticatedUserProvider.currentUser().getEmail());
        return ResponseEntity.ok(ApiResponse.success(response, "Current user retrieved"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
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
